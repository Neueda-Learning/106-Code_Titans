function updateHomeDate() {
  const currentDateElement = document.getElementById("currentDate");
  if (!currentDateElement) {
    return;
  }

  const today = new Date();
  const options = {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric"
  };

  currentDateElement.textContent = today.toLocaleDateString("en-US", options);
}

document.addEventListener("DOMContentLoaded", () => {
  updateHomeDate();
});
