package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.User;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.ForgotPasswordDTO;
import com.prashant.api.ecom.ducart.modal.ProfileDTO;
import com.prashant.api.ecom.ducart.repositories.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user() {
        User user = new User();
        user.setUserid(1L);
        user.setName("Alice");
        user.setUsername("alice");
        user.setEmail("alice@test.com");
        user.setPhone("1234567890");
        user.setPassword("plainPassword");
        user.setRole("Buyer");
        user.setAddress("Test Address");
        user.setCity("Galway");
        user.setState("Connacht");
        user.setPin("H91");
        return user;
    }

    @Test
    void signup_whenUserIsValid_shouldEncodePasswordAndSaveUser() {
        User user = user();

        when(userRepo.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(userRepo.findAll()).thenReturn(List.of());
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepo.save(user)).thenReturn(user);

        User result = userService.signup(user);

        assertEquals("encodedPassword", result.getPassword());
        assertEquals("Buyer", result.getRole());

        verify(passwordEncoder).encode("plainPassword");
        verify(userRepo).save(user);
    }

    @Test
    void signup_whenRoleIsBlank_shouldSetDefaultBuyerRole() {
        User user = user();
        user.setRole("");

        when(userRepo.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(userRepo.findAll()).thenReturn(List.of());
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepo.save(user)).thenReturn(user);

        User result = userService.signup(user);

        assertEquals("Buyer", result.getRole());
        assertEquals("encodedPassword", result.getPassword());
        verify(userRepo).save(user);
    }

    @Test
    void signup_whenRoleIsNull_shouldSetDefaultBuyerRole() {
        User user = user();
        user.setRole(null);
        when(userRepo.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(userRepo.findAll()).thenReturn(List.of());
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepo.save(user)).thenReturn(user);

        User result = userService.signup(user);

        assertEquals("Buyer", result.getRole());
    }

    @Test
    void signup_whenUserOrRequiredValuesAreNull_shouldThrowBadRequest() {
        User nullName = user();
        nullName.setName(null);
        User nullUsername = user();
        nullUsername.setUsername(null);
        User nullEmail = user();
        nullEmail.setEmail(null);
        User nullPhone = user();
        nullPhone.setPhone(null);
        User nullPassword = user();
        nullPassword.setPassword(null);

        assertThrows(BadRequestException.class, () -> userService.signup(null));
        assertThrows(BadRequestException.class, () -> userService.signup(nullName));
        assertThrows(BadRequestException.class, () -> userService.signup(nullUsername));
        assertThrows(BadRequestException.class, () -> userService.signup(nullEmail));
        assertThrows(BadRequestException.class, () -> userService.signup(nullPhone));
        assertThrows(BadRequestException.class, () -> userService.signup(nullPassword));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void signup_whenRequiredValuesAreWhitespace_shouldThrowBadRequest() {
        User blankUsername = user();
        blankUsername.setUsername("   ");
        User blankEmail = user();
        blankEmail.setEmail("   ");
        User blankPhone = user();
        blankPhone.setPhone("   ");
        User blankPassword = user();
        blankPassword.setPassword("   ");

        assertThrows(BadRequestException.class, () -> userService.signup(blankUsername));
        assertThrows(BadRequestException.class, () -> userService.signup(blankEmail));
        assertThrows(BadRequestException.class, () -> userService.signup(blankPhone));
        assertThrows(BadRequestException.class, () -> userService.signup(blankPassword));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void signup_whenAnotherUserHasDifferentPhone_shouldContinueSaving() {
        User user = user();
        User existing = new User();
        existing.setPhone("9999999999");
        when(userRepo.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(userRepo.findAll()).thenReturn(List.of(existing));
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepo.save(user)).thenReturn(user);

        User result = userService.signup(user);

        assertEquals("encodedPassword", result.getPassword());
        verify(userRepo).save(user);
    }

    @Test
    void signup_whenNameIsBlank_shouldThrowBadRequestException() {
        User user = user();
        user.setName("");

        assertThrows(BadRequestException.class, () -> userService.signup(user));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void signup_whenEmailFormatIsInvalid_shouldThrowBadRequestException() {
        User user = user();
        user.setEmail("not-an-email");

        assertThrows(BadRequestException.class, () -> userService.signup(user));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void signup_whenPhoneFormatIsInvalid_shouldThrowBadRequestException() {
        User user = user();
        user.setPhone("abc123");

        assertThrows(BadRequestException.class, () -> userService.signup(user));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void signup_whenUsernameAlreadyExists_shouldThrowConflictException() {
        User user = user();

        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(new User()));

        assertThrows(ConflictException.class, () -> userService.signup(user));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void signup_whenEmailAlreadyExists_shouldThrowConflictException() {
        User user = user();

        when(userRepo.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("alice@test.com")).thenReturn(Optional.of(new User()));

        assertThrows(ConflictException.class, () -> userService.signup(user));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void signup_whenPhoneAlreadyExists_shouldThrowConflictException() {
        User user = user();

        User existing = new User();
        existing.setPhone("1234567890");

        when(userRepo.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(userRepo.findAll()).thenReturn(List.of(existing));

        assertThrows(ConflictException.class, () -> userService.signup(user));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        User user = user();

        when(userRepo.findAll()).thenReturn(List.of(user));

        List<User> result = userService.findAll();

        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).getUsername());
        verify(userRepo).findAll();
    }

    @Test
    void findById_whenUserExists_shouldReturnUser() {
        User user = user();

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.findById(1L);

        assertEquals(1L, result.getUserid());
        assertEquals("alice", result.getUsername());
        verify(userRepo).findById(1L);
    }

    @Test
    void findById_whenUserDoesNotExist_shouldThrowResourceNotFoundException() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findById(99L));

        verify(userRepo).findById(99L);
    }

    @Test
    void updateProfile_whenUserExists_shouldUpdateProfileFields() throws Exception {
        User user = user();

        ProfileDTO dto = new ProfileDTO();
        dto.setName("Alice Updated");
        dto.setPhone("9999999999");
        dto.setAddress("New Address");
        dto.setCity("Dublin");
        dto.setState("Leinster");
        dto.setPin("D01");

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.save(user)).thenReturn(user);

        User result = userService.updateProfile(1L, dto, null);

        assertEquals("Alice Updated", result.getName());
        assertEquals("9999999999", result.getPhone());
        assertEquals("New Address", result.getAddress());
        assertEquals("Dublin", result.getCity());
        assertEquals("Leinster", result.getState());
        assertEquals("D01", result.getPin());

        verify(userRepo).save(user);
    }

    @Test
    void updateProfile_whenFieldsAreOmitted_shouldKeepExistingValues() throws Exception {
        User user = user();
        ProfileDTO dto = new ProfileDTO();
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.save(user)).thenReturn(user);

        User result = userService.updateProfile(1L, dto, null);

        assertEquals("Alice", result.getName());
        assertEquals("1234567890", result.getPhone());
        assertEquals("Test Address", result.getAddress());
        assertEquals("Galway", result.getCity());
        assertEquals("Connacht", result.getState());
        assertEquals("H91", result.getPin());
    }

    @Test
    void updateProfile_whenPicIsProvided_shouldSaveAndAssignIt() throws Exception {
        User user = user();
        ProfileDTO dto = new ProfileDTO();
        MockMultipartFile pic = new MockMultipartFile(
                "pic",
                "alice avatar.png",
                "image/png",
                "avatar".getBytes(StandardCharsets.UTF_8));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.save(user)).thenReturn(user);

        User result = userService.updateProfile(1L, dto, pic);
        Path storedFile = Path.of(
                "uploads",
                "users",
                Path.of(result.getPic()).getFileName().toString());

        try {
            assertTrue(result.getPic().startsWith("/uploads/users/"));
            assertTrue(Files.exists(storedFile));
            assertEquals("avatar", Files.readString(storedFile));
        } finally {
            Files.deleteIfExists(storedFile);
        }
    }

    @Test
    void resetPassword_whenUserExistsByUsernameAndPhoneMatches_shouldEncodeAndSaveNewPassword() {
        User user = user();

        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setUsernameOrEmail("alice");
        dto.setPhone("1234567890");
        dto.setNewPassword("newPassword");

        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepo.save(user)).thenReturn(user);

        User result = userService.resetPassword(dto);

        assertEquals("encodedNewPassword", result.getPassword());
        verify(passwordEncoder).encode("newPassword");
        verify(userRepo).save(user);
    }

    @Test
    void resetPassword_whenUserExistsByEmail_shouldEncodeAndSaveNewPassword() {
        User user = user();
        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setUsernameOrEmail("alice@test.com");
        dto.setPhone("1234567890");
        dto.setNewPassword("newPassword");
        when(userRepo.findByUsername("alice@test.com")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepo.save(user)).thenReturn(user);

        User result = userService.resetPassword(dto);

        assertEquals("encodedNewPassword", result.getPassword());
        verify(userRepo).save(user);
    }

    @Test
    void resetPassword_whenRequestOrRequiredValuesAreMissing_shouldThrowBadRequest() {
        ForgotPasswordDTO nullIdentity = new ForgotPasswordDTO();
        nullIdentity.setPhone("1234567890");
        nullIdentity.setNewPassword("newPassword");
        ForgotPasswordDTO blankIdentity = new ForgotPasswordDTO();
        blankIdentity.setUsernameOrEmail(" ");
        blankIdentity.setPhone("1234567890");
        blankIdentity.setNewPassword("newPassword");
        ForgotPasswordDTO nullPhone = new ForgotPasswordDTO();
        nullPhone.setUsernameOrEmail("alice");
        nullPhone.setNewPassword("newPassword");
        ForgotPasswordDTO blankPhone = new ForgotPasswordDTO();
        blankPhone.setUsernameOrEmail("alice");
        blankPhone.setPhone(" ");
        blankPhone.setNewPassword("newPassword");
        ForgotPasswordDTO nullPassword = new ForgotPasswordDTO();
        nullPassword.setUsernameOrEmail("alice");
        nullPassword.setPhone("1234567890");
        ForgotPasswordDTO blankPassword = new ForgotPasswordDTO();
        blankPassword.setUsernameOrEmail("alice");
        blankPassword.setPhone("1234567890");
        blankPassword.setNewPassword(" ");

        assertThrows(BadRequestException.class, () -> userService.resetPassword(null));
        assertThrows(BadRequestException.class, () -> userService.resetPassword(nullIdentity));
        assertThrows(BadRequestException.class, () -> userService.resetPassword(blankIdentity));
        assertThrows(BadRequestException.class, () -> userService.resetPassword(nullPhone));
        assertThrows(BadRequestException.class, () -> userService.resetPassword(blankPhone));
        assertThrows(BadRequestException.class, () -> userService.resetPassword(nullPassword));
        assertThrows(BadRequestException.class, () -> userService.resetPassword(blankPassword));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void resetPassword_whenUserDoesNotExist_shouldThrowResourceNotFoundException() {
        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setUsernameOrEmail("missing");
        dto.setPhone("1234567890");
        dto.setNewPassword("newPassword");

        when(userRepo.findByUsername("missing")).thenReturn(Optional.empty());
        when(userRepo.findByEmail("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.resetPassword(dto));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void resetPassword_whenPhoneDoesNotMatch_shouldThrowBadRequestException() {
        User user = user();

        ForgotPasswordDTO dto = new ForgotPasswordDTO();
        dto.setUsernameOrEmail("alice");
        dto.setPhone("0000000000");
        dto.setNewPassword("newPassword");

        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> userService.resetPassword(dto));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void update_whenUserExists_shouldUpdateUserButKeepPassword() {
        User existing = user();

        User incoming = new User();
        incoming.setName("Admin Updated");
        incoming.setUsername("adminUpdated");
        incoming.setEmail("admin@test.com");
        incoming.setPhone("1111111111");
        incoming.setPassword("shouldNotReplacePassword");
        incoming.setRole("Admin");

        when(userRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepo.save(existing)).thenReturn(existing);

        User result = userService.update(1L, incoming);

        assertEquals("Admin Updated", result.getName());
        assertEquals("adminUpdated", result.getUsername());
        assertEquals("admin@test.com", result.getEmail());
        assertEquals("1111111111", result.getPhone());
        assertEquals("plainPassword", result.getPassword());
        assertEquals("Admin", result.getRole());

        verify(userRepo).save(existing);
    }

    @Test
    void delete_whenUserExists_shouldDeleteUser() {
        when(userRepo.existsById(1L)).thenReturn(true);

        userService.delete(1L);

        verify(userRepo).existsById(1L);
        verify(userRepo).deleteById(1L);
    }

    @Test
    void delete_whenUserDoesNotExist_shouldThrowResourceNotFoundException() {
        when(userRepo.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.delete(99L));

        verify(userRepo).existsById(99L);
        verify(userRepo, never()).deleteById(99L);
    }
}
