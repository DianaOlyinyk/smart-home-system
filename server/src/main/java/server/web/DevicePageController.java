package server.web;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import server.commands.CommandAlreadyExistsException;
import server.commands.CommandService;
import server.commands.RequiredRole;
import server.devices.AccessRole;
import server.devices.Device;
import server.devices.DeviceNotFoundException;
import server.devices.DeviceService;
import server.users.User;
import server.users.UserService;

import java.security.Principal;
import java.util.UUID;

@Controller
class DevicePageController {
    private static final String COMMAND_FORM = "commandForm";
    private static final String DEVICE_VIEW = "devices/details";

    private final DeviceService deviceService;
    private final CommandService commandService;
    private final UserService userService;

    DevicePageController(
            DeviceService deviceService,
            CommandService commandService,
            UserService userService) {
        this.deviceService = deviceService;
        this.commandService = commandService;
        this.userService = userService;
    }

    @GetMapping("/devices/{id}")
    String details(
            @PathVariable("id") UUID id,
            Principal principal,
            Model model) {
        Device device = deviceService.findById(id);
        User user = userService.getUserByEmail(principal.getName());
        AccessRole accessRole = deviceService.findAccessRole(id, user.getId())
                .orElseThrow(() -> new DeviceNotFoundException(id));
        boolean isOwner = accessRole == AccessRole.OWNER;

        model.addAttribute("device", device);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("commands", commandService.findAllByDevice(
                id, isOwner ? null : RequiredRole.GUEST));
        if (!model.containsAttribute(COMMAND_FORM)) {
            model.addAttribute(COMMAND_FORM, DeviceCommandForm.empty());
        }
        return DEVICE_VIEW;
    }

    @PostMapping("/devices/{id}/commands")
    @PreAuthorize("@deviceAccessChecker.isOwner(#id, authentication)")
    String addCommand(
            @PathVariable("id") UUID id,
            @Valid @ModelAttribute(COMMAND_FORM) DeviceCommandForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return redirectWithErrors(id, form, bindingResult, redirectAttributes);
        }

        try {
            commandService.create(id, form.name(), form.argsSchema(), form.requiredRole());
        } catch (CommandAlreadyExistsException exception) {
            bindingResult.rejectValue("name", "command.duplicate",
                    "Команда з такою назвою вже існує");
            return redirectWithErrors(id, form, bindingResult, redirectAttributes);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Команду додано.");
        return "redirect:/devices/" + id;
    }

    private String redirectWithErrors(
            UUID id,
            DeviceCommandForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(COMMAND_FORM, form);
        redirectAttributes.addFlashAttribute(
                BindingResult.MODEL_KEY_PREFIX + COMMAND_FORM, bindingResult);
        return "redirect:/devices/" + id;
    }
}
