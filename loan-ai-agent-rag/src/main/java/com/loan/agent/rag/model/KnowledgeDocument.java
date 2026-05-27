package com.loan.agent.rag.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识库文档实体
 *
 * <p>一条知识包含标题、正文、分类和标签。
 * 文档会被切分成多个片段（chunk）用于检索。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {

    /** 文档唯一标识 */
    private String id;

    /** 文档标题 */
    private String title;

    /** 文档正文（完整内容） */
    private String content;

    /** 分类：还款方式 | 征信 | 利率 | 流程 | 其他 */
    private String category;

    /** 标签，用于辅助检索 */
    private List<String> tags;
}
