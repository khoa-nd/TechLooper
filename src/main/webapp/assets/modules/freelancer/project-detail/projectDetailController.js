techlooper.controller('freelancerProjectDetailController', function ($scope, utils, $location, $routeParams, apiService, $filter, resourcesService) {

  $scope.status = function (type) {
    switch (type) {
      case "get-payment-method":
        var index = resourcesService.inOptions($scope.project.payMethod, resourcesService.paymentConfig);
        if (index == -1) return "";
        return resourcesService.paymentConfig.options[index];
      case "show-fixed-price-fields":
        if (!$scope.project) return false;
        var index = resourcesService.inOptions($scope.project.payMethod, resourcesService.paymentConfig);
        if (index == -1) return false;
        return resourcesService.paymentConfig.options[index].id == "fixedPrice";

      case "show-hourly-price-fields":
        if (!$scope.project) return false;
        var index = resourcesService.inOptions($scope.project.payMethod, resourcesService.paymentConfig);
        if (index == -1) return false;
        return resourcesService.paymentConfig.options[index].id == "hourly";
    }
  }

  var parts = $routeParams.id.split("-");
  var lastPart = parts.pop();
  if (parts.length < 2 || (lastPart !== "id")) {
    return $location.path("/");
  }

  var projectId = parts.pop();
  var title = parts.join("");
  if (utils.hasNonAsciiChar(title)) {
    title = utils.toAscii(title);
    return $location.url(sprintf("/freelancer/project-detail/%s-%s-id", title, projectId));
  }

  apiService.getProject(projectId).success(function (data) {
    $scope.project = data;
    console.log($scope.project);
    //$filter("progress")($scope.contestDetail, "challenge");
  });
  var project = $.extend(true, {}, $scope.hourly, $scope.fixedPrice, $scope.project);
});

