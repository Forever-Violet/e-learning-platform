package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {



    @Value("${pay.qrcodeurl}")
    String qrcodeurl;

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;

    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    @Resource
    XcOrdersMapper ordersMapper;

    @Resource
    XcOrdersGoodsMapper ordersGoodsMapper;

    @Resource
    XcPayRecordMapper payRecordMapper;

    @Autowired
    OrderServiceImpl currentProxy;

    // 创建订单记录, 返回支付记录(包含二维码)
    @Transactional
    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {

        // 添加商品订单, 订单主表、订单明细表
        XcOrders orders = saveXcOrders(userId, addOrderDto);
        if (orders == null) {
            XueChengPlusException.cast("订单创建失败");
        }
        if (orders.getStatus().equals("600002")) {
            XueChengPlusException.cast("订单已支付");
        }
        // 生成 添加支付交易记录
        XcPayRecord payRecord = createPayRecord(orders);

        // 生成二维码
        String qrCode = null;
        try {
            // url要被模拟器访问到, url为下单接口
            String url = String.format(qrcodeurl, payRecord.getPayNo());
            qrCode = new QRCodeUtil().createQRCode(url, 200, 200);
        } catch (IOException e) {
            XueChengPlusException.cast("生成二维码出错");
        }

        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        // 填充二维码
        payRecordDto.setQrcode(qrCode);

        return payRecordDto;
    }

    // 创建商品订单
    @Transactional
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {
        // 幂等性处理
        XcOrders order = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if (order != null) { //因为订单记录只有一个, 所以如果查到, 那么直接返回, 而支付记录就可以有多个(因为可能支付失败)
            return order;
        }
        order = new XcOrders();
        // 生成订单号
        long orderId = IdWorkerUtils.getInstance().nextId();
        order.setId(orderId);
        order.setTotalPrice(addOrderDto.getTotalPrice());
        order.setCreateDate(LocalDateTime.now());
        order.setStatus("600001"); //未支付
        order.setUserId(userId);
        order.setOrderType(addOrderDto.getOrderType());
        order.setOrderName(addOrderDto.getOrderName());
        order.setOrderDetail(addOrderDto.getOrderDetail());
        order.setOrderDescrip(addOrderDto.getOrderDescrip());
        order.setOutBusinessId(addOrderDto.getOutBusinessId()); //选课记录id
        // 插入订单主表
        int i = ordersMapper.insert(order);
        if (i <= 0) {
            XueChengPlusException.cast("添加订单失败");
        }
        // 接下来将订单插入订单明细表
        String orderDetailJson = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoodsList = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        xcOrdersGoodsList.forEach(goods -> {
            XcOrdersGoods xcOrdersGoods = new XcOrdersGoods();
            // 拷贝数据
            BeanUtils.copyProperties(goods, xcOrdersGoods);
            xcOrdersGoods.setOrderId(orderId); //设置订单号
            // 插入当前订单的当前商品信息
            ordersGoodsMapper.insert(xcOrdersGoods);
        });
        return order;
    }

    // 根据业务id查询订单, 1.选课记录表中的主键
    private XcOrders getOrderByBusinessId(String outBusinessId) {
        XcOrders orders = ordersMapper.selectOne(
                new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, outBusinessId));
        return orders;
    }

    // 创建支付交易记录
    public XcPayRecord createPayRecord(XcOrders orders) {
        if (orders == null) {
            XueChengPlusException.cast("订单不存在");
        }
        if (orders.getStatus().equals("600002")) {
            XueChengPlusException.cast("订单已支付");
        }
        XcPayRecord payRecord = new XcPayRecord();
        // 生成支付交易流水号
        long payNo = IdWorkerUtils.getInstance().nextId();
        payRecord.setPayNo(payNo);
        payRecord.setOrderId(orders.getId()); //商品订单号
        payRecord.setOrderName(orders.getOrderName());
        payRecord.setTotalPrice(orders.getTotalPrice());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");//未支付
        payRecord.setUserId(orders.getUserId());
        payRecordMapper.insert(payRecord);

        return payRecord;
    }

    //根据交易记录号查询交易记录
    @Override
    public XcPayRecord getPayRecordByPayNo(String payNo) {
        return payRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
    }


    @Override
    public PayRecordDto queryPayResult(String payNo) {
        XcPayRecord payRecord = getPayRecordByPayNo(payNo);
        if (payRecord == null) {
            XueChengPlusException.cast("请重新点击支付获取二维码");
        }
        // 支付状态
        String status = payRecord.getStatus();
        // 如果支付成功直接返回
        if ("601002".equals(status)) {
            PayRecordDto payRecordDto = new PayRecordDto();
            BeanUtils.copyProperties(payRecord, payRecordDto);
            return payRecordDto;
        }
        // 从支付宝查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);
        // 保存支付结果
        currentProxy.saveAliPayStatus(payStatusDto);
        // 重新查询支付记录
        payRecord = getPayRecordByPayNo(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        return payRecordDto;

    }


    /**
     * @description 请求支付宝查询支付记录
     * @param payNo 本系统支付交易号
     * @return 支付结果
     */
    private PayStatusDto queryPayResultFromAlipay(String payNo) {
        //======请求支付宝查询支付结果======
        AlipayClient client = new DefaultAlipayClient(AlipayConfig.URL, APP_ID, APP_PRIVATE_KEY, "json",
                AlipayConfig.CHARSET, ALIPAY_PUBLIC_KEY, AlipayConfig.SIGNTYPE); // 获得初始化的AlipayClient
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo); //对于支付宝, 本系统的payNo是支付宝系统的out_trade_no
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            // 执行请求, 获得返回结果
            response = client.execute(request);
            if (!response.isSuccess()) {
                XueChengPlusException.cast("请求支付查询支付结果失败");
            }
        } catch (AlipayApiException e) {
            log.error("请求支付宝查询支付结果异常:{}", e.toString(), e);
            XueChengPlusException.cast("请求支付查询支付结果失败");
        }

        // 获取支付结果
        String resultJson = response.getBody();
        // 转为map
        Map resultMap = JSON.parseObject(resultJson, Map.class);
        Map alipay_trade_query_response = (Map) resultMap.get("alipay_trade_query_response");
        // 支付结果
        String trade_status = (String) alipay_trade_query_response.get("trade_status");
        String total_amount = (String) alipay_trade_query_response.get("total_amount");
        String trade_no = (String) alipay_trade_query_response.get("trade_no");

        // 保存支付结果
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_status(trade_status);
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTrade_no(trade_no);
        payStatusDto.setTotal_amount(total_amount);
        return payStatusDto;
    }


    /**
     * @description 保存(更新)支付宝支付结果到订单表、支付记录表
     * @param payStatusDto 支付结果信息
     */
    @Transactional
    @Override
    public void saveAliPayStatus(PayStatusDto payStatusDto) {

        // 支付流水号
        String payNo = payStatusDto.getOut_trade_no();
        XcPayRecord payRecord = getPayRecordByPayNo(payNo);
        if (payRecord == null) {
            XueChengPlusException.cast("找不到支付记录");
        }
        // 支付结果
        String trade_status = payStatusDto.getTrade_status();
        log.debug("收到支付结果:{}, 支付记录:{}", payStatusDto.toString(), payRecord.toString());
        if (trade_status.equals("TRADE_SUCCESS")) {
            // 支付金额变为分, *100
            Float totalPrice = payRecord.getTotalPrice() * 100;
            Float total_amount = Float.parseFloat(payStatusDto.getTotal_amount()) * 100;
            // 校验是否一致
            if (!payStatusDto.getApp_id().equals(APP_ID) || totalPrice.intValue() != total_amount.intValue()) {
                // 校验失败
                log.info("校验支付结果失败, 支付记录:{},APP_ID:{},totalPrice:{}", payRecord.toString(),
                        payStatusDto.getApp_id(), total_amount.intValue());
                XueChengPlusException.cast("校验支付结果失败");
            }
            log.debug("更新支付结果,支付交易流水号:{},支付结果:{}", payNo, trade_status);
            // ====更新支付记录表中的状态为支付成功, 以及其他字段====
            XcPayRecord payRecord_update = new XcPayRecord();
            payRecord_update.setStatus("601002"); //支付成功
            payRecord_update.setOutPayChannel("Alipay");
            payRecord_update.setOutPayNo(payStatusDto.getTrade_no()); //支付宝交易号
            payRecord_update.setPaySuccessTime(LocalDateTime.now()); //通知时间
            int update1 = payRecordMapper.update(payRecord_update
                    , new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
            if (update1 > 0) {
                log.info("更新支付记录状态成功:{}", payRecord_update.toString());
            } else {
                log.info("更新支付记录状态失败:{}", payRecord_update.toString());
                XueChengPlusException.cast("更新支付记录状态失败");
            }
            // 关联的订单号
            Long orderId = payRecord.getOrderId();
            XcOrders orders = ordersMapper.selectById(orderId);
            if (orders == null) {
                log.info("根据支付记录[{}]找不到订单", payRecord_update.toString());
                XueChengPlusException.cast("根据支付记录找不到订单");
            }
            
            // ====更新订单表中的订单状态为支付成功====
            XcOrders orders_update = new XcOrders();
            orders_update.setStatus("600002"); //支付成功
            int update2 = ordersMapper.update(orders_update,
                    new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getId, orderId));
            if (update2 > 0) {
                log.info("更新订单状态成功, 订单号:{}", orderId);
            } else {
                log.info("更新订单状态失败, 订单号:{}", orderId);
                XueChengPlusException.cast("更新订单表状态失败");
            }
        }

    }
}
