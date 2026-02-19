package com.sprinter.dto;

import com.sprinter.domain.enums.SystemRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO pro vytvoření nebo aktualizaci uživatele.
 */
@Data
public class UserDto {

    @NotBlank(message = "Přihlašovací jméno nesmí být prázdné")
    @Size(min = 3, max = 50, message = "Přihlašovací jméno musí mít 3-50 znaků")
    private String username;

    @NotBlank(message = "E-mail nesmí být prázdný")
    @Email(message = "E-mail není ve správném formátu")
    private String email;

    @NotBlank(message = "Křestní jméno nesmí být prázdné")
    @Size(max = 80)
    private String firstName;

    @NotBlank(message = "Příjmení nesmí být prázdné")
    @Size(max = 80)
    private String lastName;

    /** Heslo – povinné při vytváření, volitelné při aktualizaci. */
    @Size(min = 8, message = "Heslo musí mít alespoň 8 znaků")
    private String password;

    private SystemRole systemRole = SystemRole.USER;
}
