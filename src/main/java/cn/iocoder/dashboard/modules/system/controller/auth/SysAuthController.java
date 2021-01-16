package cn.iocoder.dashboard.modules.system.controller.auth;

import cn.iocoder.dashboard.common.enums.CommonStatusEnum;
import cn.iocoder.dashboard.common.pojo.CommonResult;
import cn.iocoder.dashboard.framework.logger.operatelog.core.annotations.OperateLog;
import cn.iocoder.dashboard.modules.system.controller.auth.vo.SysAuthLoginReqVO;
import cn.iocoder.dashboard.modules.system.controller.auth.vo.SysAuthLoginRespVO;
import cn.iocoder.dashboard.modules.system.controller.auth.vo.SysAuthMenuRespVO;
import cn.iocoder.dashboard.modules.system.controller.auth.vo.SysAuthPermissionInfoRespVO;
import cn.iocoder.dashboard.modules.system.convert.auth.SysAuthConvert;
import cn.iocoder.dashboard.modules.system.dal.mysql.dataobject.permission.SysMenuDO;
import cn.iocoder.dashboard.modules.system.dal.mysql.dataobject.permission.SysRoleDO;
import cn.iocoder.dashboard.modules.system.dal.mysql.dataobject.user.SysUserDO;
import cn.iocoder.dashboard.modules.system.enums.permission.MenuTypeEnum;
import cn.iocoder.dashboard.modules.system.service.auth.SysAuthService;
import cn.iocoder.dashboard.modules.system.service.permission.SysPermissionService;
import cn.iocoder.dashboard.modules.system.service.permission.SysRoleService;
import cn.iocoder.dashboard.modules.system.service.user.SysUserService;
import cn.iocoder.dashboard.util.collection.SetUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.dashboard.common.pojo.CommonResult.success;
import static cn.iocoder.dashboard.framework.security.core.util.SecurityUtils.getLoginUserId;
import static cn.iocoder.dashboard.framework.security.core.util.SecurityUtils.getLoginUserRoleIds;

@Api("认证 API")
@RestController
@RequestMapping("/")
public class SysAuthController {

    @Resource
    private SysAuthService authService;
    @Resource
    private SysUserService userService;
    @Resource
    private SysRoleService roleService;
    @Resource
    private SysPermissionService permissionService;

    @ApiOperation("使用账号密码登录")
    @PostMapping("/login")
    @OperateLog(enable = false) // 避免 Post 请求被记录操作日志
    public CommonResult<SysAuthLoginRespVO> login(@RequestBody @Valid SysAuthLoginReqVO reqVO) {
        String token = authService.login(reqVO.getUsername(), reqVO.getPassword(), reqVO.getUuid(), reqVO.getCode());
        // 返回结果
        return success(SysAuthLoginRespVO.builder().token(token).build());
    }

    @ApiOperation("获取登陆用户的权限信息")
    @GetMapping("/get-permission-info")
    @OperateLog
    public CommonResult<SysAuthPermissionInfoRespVO> getPermissionInfo() {
        // 获得用户信息
        SysUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            return null;
        }
        // 获得角色列表
        List<SysRoleDO> roleList = roleService.listRolesFromCache(getLoginUserRoleIds());
        // 获得菜单列表
        List<SysMenuDO> menuList = permissionService.listRoleMenusFromCache(getLoginUserRoleIds(),
                SetUtils.asSet(MenuTypeEnum.DIR.getType(), MenuTypeEnum.MENU.getType(), MenuTypeEnum.BUTTON.getType()),
                SetUtils.asSet(CommonStatusEnum.ENABLE.getStatus()));
        // 拼接结果返回
        return success(SysAuthConvert.INSTANCE.convert(user, roleList, menuList));
    }

    @ApiOperation("获得登陆用户的菜单列表")
    @GetMapping("list-menus")
    public CommonResult<List<SysAuthMenuRespVO>> listMenus() {
        // 获得用户拥有的菜单列表
        List<SysMenuDO> menuList = permissionService.listRoleMenusFromCache(getLoginUserRoleIds(),
                SetUtils.asSet(MenuTypeEnum.DIR.getType(), MenuTypeEnum.MENU.getType()), // 只要目录和菜单类型
                SetUtils.asSet(CommonStatusEnum.ENABLE.getStatus())); // 只要开启的
        // 转换成 Tree 结构返回
        return success(SysAuthConvert.INSTANCE.buildMenuTree(menuList));
    }

}