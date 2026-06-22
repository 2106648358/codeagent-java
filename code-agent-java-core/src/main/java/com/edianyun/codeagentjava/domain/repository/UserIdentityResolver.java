package com.edianyun.codeagentjava.domain.repository;

import com.edianyun.codeagentjava.domain.model.identity.UserIdentity;

/**
 * 用户身份解析端口（出站端口），负责解析当前操作用户的身份。
 * 默认实现通过环境变量读取，后续可对接企业认证系统。
 */
public interface UserIdentityResolver {

    /** 解析当前用户的身份信息 */
    UserIdentity resolve();
}
