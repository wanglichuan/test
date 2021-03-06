package com.h5.game.controller.admin;

import com.h5.game.controller.BaseController;
import com.h5.game.dao.base.PageResults;
import com.h5.game.model.bean.*;
import com.h5.game.services.interfaces.*;
import com.h5.game.common.tools.BaseUtil;
import com.h5.game.common.tools.validate.annotations.RequestValidate;
import com.h5.game.common.tools.validate.annotations.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * Created by 黄春怡 on 2017/4/7.
 */
@Controller
@RequestMapping(value = "/manage")
public class AdminController extends BaseController{

    @Autowired
    private AdminService adminService;
    @Autowired
    private AuthService authService;
    @Autowired
    private GameService gameService;
    @Autowired
    private UserService userService;

    @Autowired
    private GameTypeService gameTypeService;

    //登录
    @RequestMapping(value = "/login",method = RequestMethod.GET)
    @ResponseBody
    public Map<String,Object> login(@Validate String userName,@Validate String password, HttpServletRequest request){
        Map<String,Object> map = buildReturnMap(false,"");
        if(null == userName || null == password){
            map.put("status", false);
            map.put("reason", "操作失败，参数错误");
            return map;
        }
        Admin admin = adminService.getByUserName(userName);
        if(null != admin){
            if(admin.getPassword().equals(password)){
                String token = BaseUtil.getRandomToken(32);
                while( authService.getTokenCacheByToken(token) != null){
                    token = BaseUtil.getRandomToken(32);//直到生成一串不存在的token为止
                }
                authService.setAdmin(request,admin);
                authService.saveTokenCache(token,admin);//保存在session中

                map.put("token",token);
                map.put("status", true);
                map.put("reason", "登录成功");
                map.put("detail",admin);
            } else {
                map.put("status", false);
                map.put("reason", "操作失败，密码错误");
            }
        } else {
            map.put("status", false);
            map.put("reason", "操作失败，找不到该用户");
        }

        return map;
    }

    //注销
    @RequestMapping(value = "/loginOut")
    @ResponseBody
    public Map<String,Object> loginOut(HttpSession session){
        session.invalidate();
        return super.buildReturnMap(true,"注销成功");
    }

    /***超级管理员权限*********/

    //获取系统配置信息
    @RequestMapping(value = "/listConfig" ,method = RequestMethod.GET)
    @ResponseBody
    public Map<String,Object> listAdmins(){
        Map result = buildReturnMap(false,"");
        List configs = adminService.listConfig();
        if(null != configs && configs.size()>0){
            result.put("status",true);
            result.put("date",configs);
        }else {
            result.put("reason","没有找到任何配置");
        }
        return result;
    }

    //更改或添加系统配置信息
    @RequestMapping(value = "/addOrChangeConfig" ,method = RequestMethod.POST)
    @ResponseBody
    public Map<String,Object> addOrChangeConfig(){
        Map result = buildReturnMap(false,"");
        List configs = adminService.listConfig();
        if(null != configs && configs.size()>0){
            result.put("status",true);
            result.put("date",configs);
        }else {
            result.put("reason","没有找到任何配置");
        }
        return result;
    }

    //后台用户列表
    @RequestMapping(value = "/listAdmins" ,method = RequestMethod.GET)
    @ResponseBody
    public Map<String,Object> listAdmins(String userName,String realName,Integer page,Integer rows){
        PageResults pageResults = adminService.listAdmins(page,rows,userName,realName);
        if(null != pageResults && pageResults.getTotalCount() >0){
            Map result = buildReturnMap(true,"");
            result.put("rows",pageResults.getData());
            return result;
        }else {
            return buildReturnMap(false,"没有查询到任何数据");
        }
     }


    //删除后台用户
    @RequestMapping(value = "/deleteAdmin",method = RequestMethod.POST)
    @ResponseBody
    public Map<String,Object> deleteAdmin(@Validate Integer id,HttpSession session){

         Admin operator = authService.getAdmin(session);
         if(null != operator) {
             //判断用户是不是管理员
             if (operator.getRole().getId() == 1){
                 if (adminService.removeAdmin(id)) {
                     return buildReturnMap(true, "");
                 } else return buildReturnMap(false, "删除失败！");
             }else return buildReturnMap(false, "无此权限！");
         }else {
             return buildReturnMap(false, "检测到您的session失效！");
         }


    }

    //添加后台用户
    @RequestMapping(value = "/addAdmin")
    @ResponseBody
    public Map<String,Object> addAdmin(@Validate Admin admin,Integer roleId,HttpSession session){
        Admin operator = authService.getAdmin(session);
        if(null != operator) {
            //判断用户是不是管理员
            if (operator.getRole().getId() == 1) {
                return adminService.saveOrchangeAdmin(admin,roleId);
            } else return buildReturnMap(false, "没有权限！");
        }else {
            return buildReturnMap(false, "检测到您的session失效！");
        }

    }




    /********************普通管理员权限*************************/

    @RequestMapping(value = "/listUsers",method = RequestMethod.GET)
    @ResponseBody
    public Map<String,Object> listUsers(String user,String email,String wechat, Integer page, Integer rows){
        PageResults pageResults = userService.pageUsers(user,email,wechat,page,rows);
        if(null != pageResults && pageResults.getTotalCount() >0){
            Map result = buildReturnMap(true,"");
            result.put("data",pageResults.getData());
            result.put("count",pageResults.getTotalCount());
            return result;
        }else {
            return buildReturnMap(false,"没有查询到任何数据");
        }

    }


    //封禁前台用户
    @RequestMapping(value = "/lockedUser",method = RequestMethod.POST)
    @ResponseBody
    public Map<String,Object> lockedUser(@Validate Integer userId,@Validate Boolean locked){
        return userService.lockedUser(userId,locked);
    }


    //添加分类
    @RequestMapping(value = "/addGameType")
    @ResponseBody
    public Map<String,Object> addGameType(@RequestValidate GameType gameType){
        return gameTypeService.saveOrUpdateUser(gameType);
    }

    //更改游戏(主要是推荐、审核)
    @RequestMapping(value = "/checkGame")
    @ResponseBody
    public Map<String,Object> checkGame(Game game){
        return gameService.updateGame(game);
    }

    //删除评论
    @RequestMapping(value = "/removeComment")
    @ResponseBody
    public Map<String,Object> removeComment(Comment comment){
        if(gameService.removeComment(comment)){
            return buildReturnMap(true,"删除成功！");
        }else {
            return buildReturnMap(false,"删除失败！");
        }
    }

    //删除游戏
    @RequestMapping(value = "/removeGame",method = RequestMethod.POST)
    @ResponseBody
    public Map removeGame(@Validate Integer gameId) {
        Boolean result = gameService.removeGame(gameId);
        if(result){
            return buildReturnMap(true,"删除成功！");
        }else return buildReturnMap(false,"删除失败！");
    }





}
