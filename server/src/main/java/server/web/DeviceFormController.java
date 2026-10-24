package server.web;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import server.devices.Device;
import server.devices.DeviceService;
import server.devices.DeviceType;
import server.users.User;

@Controller
class DeviceFormController {

    private final DeviceService deviceService;

    DeviceFormController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/devices/new")
    String form(Model model) {
        model.addAttribute("form", new CreateDeviceForm(null, null));
        model.addAttribute("types", DeviceType.values());
        return "devices/new";
    }

    @PostMapping("/devices")
    String create(@AuthenticationPrincipal User user,
                  @Valid @ModelAttribute("form") CreateDeviceForm form,
                  BindingResult bindingResult,
                  Model model,
                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("types", DeviceType.values());
            return "devices/new";
        }
        Device device = deviceService.create(form.name().strip(), form.type(), user.getId());
        redirectAttributes.addFlashAttribute("success", "Пристрій «" + device.getName() + "» додано");
        return "redirect:/devices";
    }
}
