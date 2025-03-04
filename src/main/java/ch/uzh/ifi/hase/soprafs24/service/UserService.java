package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  public User createUser(User newUser) {
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setStatus(UserStatus.ONLINE);
    checkIfUserExists(newUser);
    newUser.setCreationDate(LocalDate.now());
    // saves the given entity but data is only persisted in the database once
    // flush() is called
    newUser = userRepository.save(newUser);
    userRepository.flush();

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }

  /**
   * This is a helper method that will check the uniqueness criteria of the
   * username and the name
   * defined in the User entity. The method will do nothing if the input is unique
   * and throw an error otherwise.
   *
   * @param userToBeCreated
   * @throws org.springframework.web.server.ResponseStatusException
   * @see User
   */
  private void checkIfUserExists(User userToBeCreated) {
    User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
    //User userByName = userRepository.findByName(userToBeCreated.getName());
    if (userByUsername != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
    }
  }
  // Login method: Validates if the user exists and if the password (stored in the "name" field) matches.
  public User loginUser(String username, String password) {
    // 1. Find user by username.
    User user = userRepository.findByUsername(username);
    if (user == null) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User does not exist.");
    }
    // 2. Validate the password (here, the "name" field is used as the password).
    if (!user.getName().equals(password)) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password.");
    }
    user.setStatus(UserStatus.ONLINE);
    userRepository.save(user);
    return user;
  }

  public User getUserById(Long userId) {
    return userRepository.findById(userId).orElse(null);
  }

  public void logoutUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    user.setStatus(UserStatus.OFFLINE);
    userRepository.save(user);
  }

  public void updateUser(Long userId, User userData) {
    User existingUser = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (userData.getUsername() != null && !userData.getUsername().isEmpty()) {
        User userByUsername = userRepository.findByUsername(userData.getUsername());
        if (userByUsername != null && !userByUsername.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken");
        }
        existingUser.setUsername(userData.getUsername());
    }
    if (userData.getBirthday() != null) {
        existingUser.setBirthday(userData.getBirthday());
    }

    userRepository.save(existingUser);
  }

}
