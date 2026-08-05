function setActiveNavigationLink() {
  const currentPage = window.location.pathname.split("/").pop() || "index.html";
  const pageFile = currentPage.split("?")[0];

  document.querySelectorAll(".nav-link").forEach((link) => {
    const href = link.getAttribute("href");
    if (href === pageFile || (pageFile === "" && href === "index.html")) {
      link.classList.add("active");
    } else {
      link.classList.remove("active");
    }
  });
}

function setCurrentDate() {
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

async function loadSharedHeader() {
  const container = document.getElementById("header-nav-container");
  if (!container) {
    return;
  }

  try {
    const response = await fetch("common.html");
    if (!response.ok) {
      throw new Error(`Header load failed: ${response.status}`);
    }

    container.innerHTML = await response.text();
    setActiveNavigationLink();
    setCurrentDate();
  } catch (error) {
    console.error("Error loading header:", error);
  }
}

document.addEventListener("DOMContentLoaded", () => {
  loadSharedHeader();
});

window.loadSharedHeader = loadSharedHeader;
