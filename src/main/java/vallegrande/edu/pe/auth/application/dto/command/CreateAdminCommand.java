package vallegrande.edu.pe.auth.application.dto.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminCommand {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El apellido es obligatorio")
    private String lastname;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @NotBlank(message = "El DNI es obligatorio")
    private String dni;

    private String phone;

    private String role; // SUPERADMIN, PARROCO, SECRETARIO

    private Long tenantId; // Opcional para SUPERADMIN, obligatorio para PARROCO
}
