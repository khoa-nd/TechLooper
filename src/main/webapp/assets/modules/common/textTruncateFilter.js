techlooper.filter("textTruncate", function () {
  return function (text, type) {
    if (!text) return "";

    switch (type) {
      case "email":
        return text.replace(/@.*/, "");

      case "display-text":
        return text.split(" ")[0];
    }
    return text;
  };
});