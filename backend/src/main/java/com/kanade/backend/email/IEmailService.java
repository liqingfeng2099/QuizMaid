package com.kanade.backend.email;

import java.io.File;
import java.util.List;

public interface IEmailService {



    /**
     * 普通邮件发送
     *
     * @param form    发送人
     * @param to      发送对象
     * @param subject 主题
     * @param content 内容
     */
    void sendText(String form, String to, String subject, String content);

    /**
     * Html邮件发送
     *
     * @param form    发送人
     * @param to      发送对象
     * @param subject 主题
     * @param content 内容
     */
    void sendHtml(String form, String to, String subject, String content);

    /**
     * 邮件发送
     *
     * @param name    发送人名称
     * @param form    发送人
     * @param to      发送对象
     * @param subject 主题
     * @param content 内容
     * @param isHtml  是否为html
     * @param cc      抄送，多人用逗号隔开
     * @param bcc     密送，多人用逗号隔开
     * @param files   文件
     */
    void send(String name, String form, String to, String subject, String content, Boolean isHtml, String cc, String bcc, List<File> files);




}
