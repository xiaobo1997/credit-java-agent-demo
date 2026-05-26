package com.loan.agent.evaluate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评测用例：输入借款申请 + 期望的审核结果关键词。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {
    /** 用例名称 */
    private String name;
    /** 姓名 */
    private String applicantName;
    /** 身份证号（尾号决定征信分数：300 + 尾号*55） */
    private String idCard;
    /** 申请金额 */
    private String amount;
    /** 借款用途 */
    private String purpose;
    /** 期望结果包含的关键词 */
    private String expectedKeyword;
    /** 期望的征信分数（用于校验工具是否被调用） */
    private int expectedScore;
}
