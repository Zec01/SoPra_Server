package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  private User testUser;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    testUser = new User();
    testUser.setId(1L);
    testUser.setPassword("User.1234");
    testUser.setUsername("testUsername");
    Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
  }

  @Test
  public void createUser_validInputs_success() {
    User createdUser = userService.createUser(testUser);
    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
    assertEquals(testUser.getId(), createdUser.getId());
    assertEquals(testUser.getPassword(), createdUser.getPassword());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.ONLINE, createdUser.getStatus());
  }

  @Test
  public void createUser_duplicateInputs_throwsException() {
    userService.createUser(testUser);
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void loginUser_success() {
    Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(testUser);
    User loggedIn = userService.loginUser("testUsername", "testName");
    assertEquals(UserStatus.ONLINE, loggedIn.getStatus());
    Mockito.verify(userRepository).save(testUser);
  }

  @Test
  public void loginUser_incorrectPassword_throwsException() {
    Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(testUser);
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
      userService.loginUser("testUsername", "wrongPass");
    });
    assertEquals("Incorrect password.", ex.getReason());
  }

  @Test
  public void loginUser_userNotFound_throwsException() {
    Mockito.when(userRepository.findByUsername("noSuchUser")).thenReturn(null);
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
      userService.loginUser("noSuchUser", "somePass");
    });
    assertEquals("User does not exist.", ex.getReason());
  }

  @Test
  public void getUserById_found() {
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    User found = userService.getUserById(1L);
    assertNotNull(found);
    assertEquals("testUsername", found.getUsername());
  }

  @Test
  public void getUserById_notFound() {
    Mockito.when(userRepository.findById(99L)).thenReturn(Optional.empty());
    User found = userService.getUserById(99L);
    assertNull(found);
  }

  @Test
  public void logoutUser_success() {
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    userService.logoutUser(1L);
    assertEquals(UserStatus.OFFLINE, testUser.getStatus());
    Mockito.verify(userRepository).save(testUser);
  }

  @Test
  public void logoutUser_notFound() {
    Mockito.when(userRepository.findById(99L)).thenReturn(Optional.empty());
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
      userService.logoutUser(99L);
    });
    assertEquals("User not found", ex.getReason());
  }

  @Test
  public void updateUser_validInputs_success() {
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    User newData = new User();
    newData.setUsername("updatedName");
    newData.setBirthday(LocalDate.of(1999, 12, 31));
    userService.updateUser(1L, newData);
    assertEquals("updatedName", testUser.getUsername());
    assertEquals("1999-12-31", testUser.getBirthday().toString());
    Mockito.verify(userRepository).save(testUser);
  }

  @Test
  public void updateUser_notFound() {
    Mockito.when(userRepository.findById(99L)).thenReturn(Optional.empty());
    User newData = new User();
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
      userService.updateUser(99L, newData);
    });
    assertEquals("User not found", ex.getReason());
  }

  @Test
  public void updateUser_duplicateUsername_throwsException() {
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    User otherUser = new User();
    otherUser.setId(2L);
    Mockito.when(userRepository.findByUsername("duplicateUsername")).thenReturn(otherUser);
    User newData = new User();
    newData.setUsername("duplicateUsername");
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
      userService.updateUser(1L, newData);
    });
    assertEquals("Username already taken", ex.getReason());
  }
}
