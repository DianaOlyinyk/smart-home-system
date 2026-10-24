package server.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import server.devices.DeviceService;
import server.users.User;

@Controller
class DeviceListController {

    private final DeviceService deviceService;

    DeviceListController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/devices")
    String list(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("devices", deviceService.findAllForUser(user.getId()));
        return "devices/list";
    }
}
