techlooper.factory("securityService", function (apiService, $rootScope, $q, utils, jsonValue, $location, localStorageService) {

  //localStorage.setItem('CAPTURE-PATHS', '/');

  var instance = {
    logout: function () {
      apiService.logout()
        .success(function (data, status, headers, config) {
          $.removeCookie("JSESSIONID");
          $rootScope.userInfo = undefined;
          var roles = $rootScope.currentUiView.roles || [];
          if (roles.length > 0) {
            $location.url("/");
          }
        });
    },

    getCurrentUser: function (type) {
      if ($rootScope.userInfo) {
        var deferred = $q.defer();
        deferred.resolve($rootScope.userInfo);
        return deferred.promise;
      }

      $rootScope.userInfo = undefined;
      //utils.sendNotification(jsonValue.notifications.loading, $(window).height());
      return apiService.getCurrentUser(type)
        .success(function (data) {
          //utils.sendNotification(jsonValue.notifications.loaded, $(window).height());

          $rootScope.userInfo = data;

          //var lastFoot = localStorageService.get("lastFoot");
          //if (lastFoot && ["/login", "/user-type"].indexOf(lastFoot) == -1) {
          //  localStorageService.remove("lastFoot");
          //  return $location.path(lastFoot);
          //}
          //localStorageService.remove("lastFoot");

          //instance.routeByRole();
        });
      //.error(function () {
      //  utils.sendNotification(jsonValue.notifications.loaded, $(window).height());});
    },

    login: function (username, password, type) {
      var auth = type ? {us: username, pwd: password} : {
        us: $.base64.encode(username),
        pwd: $.base64.encode(password)
      };
      return apiService.login(auth)
        .success(function (data, status, headers, config) {
          instance.getCurrentUser().then(function () {
            instance.routeByRole();
          });
        })
        .error(function () {
          if ($rootScope.currentUiView && $rootScope.currentUiView.loginUrl) {
            $location.url($rootScope.currentUiView.loginUrl);
          }
          //else {
          //  $location.url("/");
          //}
          //utils.sendNotification(jsonValue.notifications.loaded);
          utils.sendNotification(jsonValue.notifications.loaded);
        });
    },

    routeByRole: function () {
      utils.sendNotification(jsonValue.notifications.loaded);
      var lastFoot = localStorageService.get("lastFoot");
      var uiView = utils.getUiView(lastFoot);

      if (lastFoot && !uiView.ignoreIfLastFoot) {
        return $location.url(lastFoot);
      }

      switch ($rootScope.userInfo.roleName) {
        case "EMPLOYER":
          return $location.url("/employer-dashboard");

        case "JOB_SEEKER":
          return $location.url("/home");
      }
    },

    initialize: function () {
      $rootScope.$on("$locationChangeSuccess", function (event, next, current) {
        var uiView = utils.getUiView();
        if (uiView.name == "rootPage") {
          return false;
        }

        var param = $location.search();
        var notHasParam = !$.isEmptyObject(param);

        var isSignInView = uiView.type == "LOGIN";
        var notSignedIn = !$rootScope.userInfo && isSignInView;
        if (notSignedIn && notHasParam) {// should keep last print
          localStorageService.set("lastFoot", $location.url());
        }

        if (!isSignInView) localStorageService.set("priorFoot", $location.url());
      });

      $rootScope.$on("$routeChangeSuccess", function (event, next, current) {
        $rootScope.currentUiView = utils.getUiView();
        var isSignInView = $rootScope.currentUiView.type == "LOGIN";
        var priorFoot = localStorageService.get("priorFoot");
        if (isSignInView) {
          return localStorageService.set("lastFoot", priorFoot);
        }

        localStorageService.set("priorFoot", $location.url());
        localStorageService.set("lastFoot", $location.url());
      });

      $rootScope.$on("$routeChangeStart", function (event, next, current) {
        var uiView = utils.getUiView();
        var roles = uiView.roles || [];
        if ($rootScope.userInfo) {
          var noPermission = roles.length > 0 && roles.indexOf($rootScope.userInfo.roleName) < 0;
          if (noPermission) {
            alert("Your current account is not authorized to access that feature. Please use your VietnamWorks employer account instead.");
            return event.preventDefault();
          }
          return false;
        }

        //$rootScope.currentUiView = utils.getUiView();
        if (roles.length > 0) {//is protected pages
          instance.getCurrentUser().error(function (userInfo) {
            return $location.url(uiView.loginUrl);
          });
        }

      });

      if (!$rootScope.userInfo) instance.getCurrentUser();
    }
  };

  return instance;
});