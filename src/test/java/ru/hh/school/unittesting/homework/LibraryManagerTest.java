package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

  @Mock
  private NotificationService notificationService;
  @Mock
  private UserService userService;
  @InjectMocks
  private LibraryManager libraryManager;

  @BeforeEach
  void setUp() {
    libraryManager.addBook("book1", 1);
    libraryManager.addBook("book out of stock", 0);
  }

  @Test
  @Disabled
  void addBookShouldNotChangeAvailableCopyToNegativeValue() {
    assertThatThrownBy(() -> libraryManager.addBook("book1", -2));
  }

  @Test
  void addBookShouldAddNewBooks() {
    libraryManager.addBook("new book id", 1);

    int availableBook = libraryManager.getAvailableCopies("new book id");

    assertThat(availableBook).isEqualTo(1);
  }

  @Test
  void addBookShouldChangeQuantityOfExistingBooks() {
    libraryManager.addBook("book1", 1);

    int availableBook = libraryManager.getAvailableCopies("book1");

    assertThat(availableBook).isEqualTo(2);
  }

  @Test
  void borrowBookShouldReturnFalseAndNotifyNotActiveUser() {
    when(userService.isUserActive(any())).thenReturn(false);

    var isBorrowed = libraryManager.borrowBook("", "user1");

    assertThat(isBorrowed).isFalse();
    verify(notificationService, times(1)).notifyUser("user1", "Your account is not active.");
  }

  @ParameterizedTest
  @CsvSource({
      "book out of stock, user1",
      "not found book, user1"
  })
  void borrowBookShouldReturnFalseWhenNoBooks(String bookId, String userId) {
    when(userService.isUserActive(userId)).thenReturn(true);

    var isBorrowed = libraryManager.borrowBook(bookId, userId);

    assertThat(isBorrowed).isFalse();
  }

  @Test
  void borrowBookShouldReturnTrueAndNotifyUser() {
    when(userService.isUserActive(any())).thenReturn(true);

    var isBorrowed = libraryManager.borrowBook("book1", "user1");

    assertThat(isBorrowed).isTrue();
    verify(notificationService, times(1)).notifyUser("user1", "You have borrowed the book: book1");
  }

  @Test
  void returnBookShouldReturnFalseWhenBookNotBorrowed() {
    assertThat(libraryManager.returnBook("book1", "user1")).isFalse();
  }

  @Test
  void returnBookShouldReturnFalseWhenBookBorrowedByOtherUser() {
    when(userService.isUserActive("user1")).thenReturn(true);
    libraryManager.borrowBook("book1", "user1");

    var isReturned = libraryManager.returnBook("book1", "user2");

    assertThat(isReturned).isFalse();
  }

  @Test
  void returnBookShouldReturnTrueAndNotifyUser() {
    when(userService.isUserActive("user1")).thenReturn(true);
    libraryManager.borrowBook("book1", "user1");

    var isReturned = libraryManager.returnBook("book1", "user1");

    assertThat(isReturned).isTrue();
    verify(notificationService, times(1)).notifyUser("user1", "You have returned the book: book1");
  }

  @Test
  void calculateDynamicLateFeeShouldThrowExceptionIfOverdueDaysIsNegative() {
    assertThatThrownBy(() -> libraryManager.calculateDynamicLateFee(-1, false, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Overdue days cannot be negative.");
  }

  @ParameterizedTest
  @CsvSource({
      "1, false, false, 0.5",
      "20, false, false, 10",
      "20, true, false, 15",
      "20, false, true, 8",
      "20, true, true, 12"
  })
  void calculateDynamicLateFeeShouldReturnValue(
      int overdueDays,
      boolean isBestseller,
      boolean isPremiumMember,
      double expectedLateFee
  ) {
    double lateFee = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);

    assertThat(lateFee).isEqualTo(expectedLateFee);
  }
}