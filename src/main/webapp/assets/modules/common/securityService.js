techlooper.factory("securityService", function (apiService, $rootScope, $q, utils, jsonValue, $location) {

  localStorage.setItem('CAPTURE-PATHS', '/');

  var instance = {
    //logout: function () {
    //  apiService.logout()
    //    .success(function (data, status, headers, config) {
    //      $rootScope.userInfo = undefined;
    //      $location.path("/");
    //    });
    //},

    login: function (username, password) {
      $.cookie("us", $.base64.encode(username));
      $.cookie("pwd", $.base64.encode(password));

      apiService
        .login({us: $.cookie("us"), pwd: $.cookie("pwd")})
        .success(function (data, status, headers, config) {
            $rootScope.$emit("$loginSuccess");
          })
        .error(function(data, status, headers, config) {
          $rootScope.$emit("$loginFailed");
        });
      $.removeCookie("us");
      $.removeCookie("pwd");
    },

    getCurrentUser: function () {
      var deffer = $q.defer();
      apiService.getCurrentUser().success(function(data) {
        $rootScope.userInfo = data;
        deffer.resolve(data);
      });
      return deffer.promise;
    },

    ableToGo: function () {
      instance.getCurrentUser()
        .then(function (data) {
          var path = localStorage.getItem('CAPTURE-PATHS');
          if (path) {
            return $location.path(path);
          }
          return $location.path("/");
        })
        .catch(function () {return $location.path("/login");});
      utils.sendNotification(jsonValue.notifications.hideLoadingBox);
    },

    init: function(){}
  };

  $rootScope.$on("$loginSuccess", function () {
    instance.ableToGo();
  });


  return instance;
});