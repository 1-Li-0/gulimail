package com.gulimall.cart.controller;

import com.example.common.constant.AuthServerConstant;
import com.example.common.utils.R;
import com.gulimall.cart.interceptor.CartInterceptor;
import com.gulimall.cart.service.CartService;
import com.gulimall.cart.vo.CartItemVo;
import com.gulimall.cart.vo.CartVo;
import com.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {
    @Autowired
    private CartService cartService;

    @GetMapping("/getCurrentUserItems")
    public @ResponseBody List<CartItemVo> getCurrentUserItems(){
        return cartService.getCurrentUserItems();
    }

    //删除购物项
    @GetMapping("/deleteItem.do")
    @ResponseBody
    public R deleteItem(@RequestParam(value = "skuId") Long skuId) throws ExecutionException, InterruptedException {
        CartVo cart = cartService.deleteItem(skuId);
        return R.ok().put("cart", cart);
    }

    //修改购物项
    @GetMapping("/updateItem.do")
    @ResponseBody
    public R updateItem(@RequestParam(value = "skuId", required = true) Long skuId,
                        @RequestParam(value = "check", required = false) Integer check
                        , @RequestParam(value = "count", required = false) Integer count) throws ExecutionException, InterruptedException {
        CartVo cart = cartService.updateItem(skuId, check, count);
        return R.ok().put("cart", cart);
    }

    /**
     * @return 跳转到购物车页面
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        CartVo cart = cartService.getCart();
        model.addAttribute("cart", cart);
        return "cartList";
    }

    /**
     * @return 添加到购物车
     */
    @GetMapping("/addToCart")
    public String cartList(@RequestParam("skuId") Long skuId,
                           @RequestParam("num") Integer num,
                           RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        cartService.addToCart(skuId, num);
        redirectAttributes.addAttribute("skuId", skuId);
        //重定向到成功页面，防止刷新页面时重复发送请求
        return "redirect:http://cart.gulimall.com/addToCartSuccess.do";
    }

    /**
     * @return 跳转到添加成功的页面，展示购物项信息
     */
    @GetMapping("/addToCartSuccess.do")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, Model model) {
        CartItemVo item = cartService.getCartItem(skuId);
        model.addAttribute("item", item);
        return "success";
    }
}
