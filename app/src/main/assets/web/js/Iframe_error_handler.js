/*
Credits: Mubashir P A
Last updated: 04/12/2021
*/
window.addEventListener("DOMContentLoaded", function () {
    const iframe_tag = document.getElementsByTagName("iframe");
    for (let i = 0; i < iframe_tag.length; i++) {
        iframe_tag[i].onload = function () {
            try {
                if (iframe_tag[i].contentWindow.location.href == "chrome-error://chromewebdata/") {
                    console.log("Error: " + iframe_tag[i].src);
                    iframe_tag[i].src = "about:blank";
                }
            } catch (err) {
                console.log(err);
            }
        };
    }
});
