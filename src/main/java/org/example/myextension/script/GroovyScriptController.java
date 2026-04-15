package org.example.myextension.script;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.example.myextension.auth.manage.ManageAuthPermission;
import org.example.myextension.utils.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("/script/groovy")
@ConditionalOnProperty(name = "extension.script.enable", havingValue = "true")
public class GroovyScriptController {

    private static final Logger log = LoggerFactory.getLogger(GroovyScriptController.class);

    @GetMapping("/index")
    public ModelAndView index(ModelAndView modelAndView) {
        modelAndView.setViewName("react-pages/online_script");
        return modelAndView;
    }

    @PostMapping("/exec")
    @ManageAuthPermission(shopMatch = false)
    public ResponseEntity<Object> exec(@RequestBody String command, HttpServletRequest request) {
        try {
            Binding binding = new Binding();
            binding.setVariable("applicationContext", SpringContextUtil.getApplicationContext());

            GroovyShell shell = new GroovyShell(binding);
            Script script = shell.parse(command);
            return ResponseEntity.ok(script.run());
        } catch (Exception e) {
            log.error("脚本执行失败", e);
            // 这里对应 Kotlin 的 e.stackTraceToString()
            return ResponseEntity.ok(getStackTraceAsString(e));
        }
    }

    @PostMapping("/exec/file")
    @ManageAuthPermission(shopMatch = false)
    public ResponseEntity<Object> execFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            String suffix = FileUtil.getSuffix(file.getOriginalFilename());
            if (suffix != null && !suffix.isEmpty()) {
                return ResponseEntity.badRequest().body("不支持的文件类型: " + suffix);
            }

            Binding binding = new Binding();
            binding.setVariable("applicationContext", SpringContextUtil.getApplicationContext());

            String scriptContent = IoUtil.readUtf8(file.getInputStream());
            GroovyShell shell = new GroovyShell(binding);
            Script script = shell.parse(scriptContent);
            return ResponseEntity.ok(script.run());
        } catch (Exception e) {
            log.error("脚本执行失败", e);
            return ResponseEntity.ok(getStackTraceAsString(e));
        }
    }

    /**
     * 模拟 Kotlin 的 Throwable.stackTraceToString() 扩展函数
     */
    private String getStackTraceAsString(Throwable e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}