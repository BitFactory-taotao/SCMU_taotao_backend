package com.bit.scmu_taotao;

import com.bit.scmu_taotao.entity.*;
import com.bit.scmu_taotao.service.*;
import com.bit.scmu_taotao.util.common.Result;
import com.bit.scmu_taotao.util.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户接口测试类
 * 测试 getFavorites 和 getSellGoods 接口
 */
@SpringBootTest
@MapperScan("com.bit.scmu_taotao.mapper")
class UserControllerTest {

}