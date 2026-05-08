package cn.bugstack.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件内容响应
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SshFileContentResponseDTO {

    /** 文件绝对路径 */
    private String path;

    /** 文件名 */
    private String name;

    /** 文件编码，默认 UTF-8 */
    private String charset;

    /** 文件大小（字节） */
    private Long size;

    /** 是否疑似二进制 */
    private boolean binary;

    /** 是否因为太大被截断 */
    private boolean truncated;

    /** 文件内容（文本） */
    private String content;

}
