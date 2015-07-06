techlooper.controller('contestDetailController', function ($scope, apiService, localStorageService, $location, $routeParams) {

  var contestId = $routeParams.id;

  $scope.status = function(type) {
    switch (type) {
      case "able-to-join":
        var joinContests = localStorageService.get("joinContests") || "";
        var registerVnwUser = localStorageService.get("registerVnwUser") || "";
        return joinContests.indexOf(contestId) < 0 || registerVnwUser.length == 0;
    }
  }

  $scope.joinNowByFB = function() {
    if (!$scope.status('able-to-join')) {
      return false;
    }

    localStorageService.set("lastFoot", $location.url());
    apiService.getFBLoginUrl().success(function(url) {
      var joinContests = localStorageService.get("joinContests") || "";
      if (joinContests.indexOf(contestId) < 0) {
        joinContests += joinContests.length > 0 ? "," + contestId : contestId;
      }
      localStorageService.set("joinContests", joinContests);
      localStorageService.set("lastFoot", $location.url());
      localStorageService.set("joinNow", true);
      window.location = url;
    });
  }

  if (localStorageService.get("joinNow")) {
    localStorageService.remove("joinNow")
    apiService.joinContest(contestId, localStorageService.get("registerVnwUser"))
      .success(function(data) {
        console.log(data);
      });
  }

  apiService.getContestDetail(contestId).success(function(data) {
    $scope.contestDetail = data;
    $scope.contestDetail.countDown = parseInt(moment().countdown($scope.formatDate($scope.contestDetail.startDateTime), countdown.DAYS, NaN, 2).toString());
    //$scope.contestDetail.status = moment($scope.contestDetail.startDateTime).toNow();
    //console.log($scope.contestDetail.status);
  });
  $scope.formatDate = function(d){
    return moment(d).format('MM DD YYYY');
  }
});

