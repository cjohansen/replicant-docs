(function () {

  function initTableOfContents() {
    var tocContainer = document.getElementById("table-of-contents-content");
    if (!tocContainer) return;

    // Get all heading elements that are referenced in the ToC
    var tocLinks = tocContainer.querySelectorAll('a[href^="#"]');
    if (tocLinks.length === 0) return;

    var allArticleHeadings = Array.from(document.querySelectorAll('h2, h3'))
    var headings = [];
    for (var i = 0; i < tocLinks.length; i++) {
      var link = tocLinks[i];
      var hash = link.getAttribute("href");
      var id = hash.substring(1); // Remove the # from href

      // Use getElementById instead of querySelector to handle special characters
      var heading = document.getElementById(id) ||
                    // try to find element by text (non-blocks content doens't have normalized ids)
                    allArticleHeadings.filter(h => h.textContent.trim() === link.textContent.trim())[0]

      if (heading) {
        headings.push({
          link: link,
          element: heading,
          id: id
        });

        // Add smooth scroll behavior to ToC links
        link.addEventListener("click", function(e) {
          e.preventDefault();

          // Calculate the target position with padding
          var elementTop = this.headingElement.getBoundingClientRect().top + window.pageYOffset;
          var offsetTop = elementTop - 80; // 80px padding from top

          window.scrollTo({
            top: offsetTop,
            behavior: "smooth"
          });
        }.bind({ headingElement: heading }));
      }
    }

    if (headings.length === 0) return;

    function updateActiveTocItem() {
      var scrollTop = window.pageYOffset || document.documentElement.scrollTop;
      var windowHeight = window.innerHeight;

      // Find the current active section
      var activeIndex = -1;

      for (var i = 0; i < headings.length; i++) {
        var rect = headings[i].element.getBoundingClientRect();
        var elementTop = rect.top + scrollTop;

        // Consider a heading active if it's above the middle of the viewport
        if (elementTop <= scrollTop + windowHeight / 3) {
          activeIndex = i;
        } else {
          break;
        }
      }

      // Update ToC link styles
      for (var j = 0; j < headings.length; j++) {
        var heading = headings[j];
        var link = heading.link;
        var isActive = j === activeIndex;

        if (isActive) {
          // Add active styles - text-primary color
          link.classList.add("text-primary");
        } else {
          // Reset to default stylesheet
          link.classList.remove("text-primary");
        }
      }
    }

    // Initial update
    updateActiveTocItem();

    // Update on scroll with throttling for better performance
    var ticking = false;
    function handleScroll() {
      if (!ticking) {
        requestAnimationFrame(function() {
          updateActiveTocItem();
          ticking = false;
        });
        ticking = true;
      }
    }

    window.addEventListener("scroll", handleScroll, { passive: true });
  }

  // Initialize when DOM is ready
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", function () {
      initTableOfContents();
    });
  } else {
    initTableOfContents();
  }
})();
