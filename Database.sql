create table email_code
(
    email       varchar(255) null comment '邮箱地址',
    code        varchar(255) null comment '验证码',
    create_time datetime     null comment '验证码创建时间',
    status      int          null comment '验证码状态，0: 未使用  1: 已使用'
);

create table file_collect
(
    collect_id  varchar(255) not null comment '收藏记录 ID',
    user_id     varchar(255) null comment '收藏文件的用户 ID',
    file_id     varchar(255) not null comment '被收藏的文件 ID',
    create_time datetime     not null comment '收藏时间'
);

create table file_info
(
    file_id          varchar(255)            null comment '文件 ID',
    user_id          varchar(255)            null comment '文件所属用户 ID',
    file_md5         varchar(255)            null comment '文件的 MD5 哈希值',
    file_pid         varchar(255)            null comment '文件父目录 ID',
    file_size        bigint                  null comment '文件大小',
    file_name        varchar(255)            null comment '文件名称',
    file_cover       varchar(255)            null comment '文件封面地址',
    file_path        varchar(255)            null comment '文件存储路径',
    create_time      datetime                null comment '文件创建时间',
    last_update_time datetime                null comment '文件最后更新时间',
    folder_type      int                     null comment '文件类型，0: 文件 1: 目录',
    file_category    int                     null comment '文件分类，1: 视频 2: 音频  3: 图片 4: 文档 5: 其他',
    file_type        int                     null comment '具体文件类型，1: 视频 2: 音频  3: 图片 4: pdf 5: doc 6: excel 7: txt 8: code 9: zip 10: 其他',
    status           int                     null comment '文件转码状态，0: 转码中 1: 转码失败 2: 转码成功',
    recovery_time    datetime                null comment '文件放入回收站的时间',
    del_flag         int                     null comment '删除标记，0: 删除  1: 回收站  2: 正常',
    permission       int          default 0  not null comment '权限类别，0: 个人 1: 公开',
    access_type      int          default 1  not null comment '权限类型，0: 只读下载 1: 可读写删下载',
    create_by        varchar(255)            null comment '上传人',
    description      varchar(255) default '' null comment '文件介绍，最多100字'
);

create table file_share
(
    share_id    varchar(255) null comment '分享 ID',
    file_id     varchar(255) null comment '被分享的文件 ID',
    user_id     varchar(255) null comment '分享文件的用户 ID',
    valid_type  int          null comment '分享有效期类型，可自定义对应有效期，如 0: 1 天 1: 7 天 2: 30 天 3: 永久有效',
    expire_time datetime     null comment '分享失效时间',
    share_time  datetime     null comment '文件分享时间',
    code        varchar(255) null comment '分享提取码',
    show_count  int          null comment '分享链接的浏览次数'
);

create table user_info
(
    user_id         varchar(255) null comment '用户 ID',
    nick_name       varchar(255) null comment '用户昵称',
    email           varchar(255) null comment '用户邮箱',
    qq_open_id      varchar(255) null comment '用户 QQ 开放平台 ID',
    qq_avatar       varchar(255) null comment '用户 QQ 头像地址',
    password        varchar(255) null comment '用户密码',
    join_time       datetime     null comment '用户加入时间',
    last_login_time datetime     null comment '用户最后登录时间',
    status          int          null comment '用户状态，0: 禁用 1: 正常',
    use_space       bigint       null comment '用户已使用空间，单位 byte',
    total_space     bigint       null comment '用户总空间'
);

