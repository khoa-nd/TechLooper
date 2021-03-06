techlooper.controller('freelancerPostProjectController', function ($scope, jsonValue, resourcesService, $rootScope,
                                                                   apiService, $location, utils, $translate, localStorageService) {
  utils.sendNotification(jsonValue.notifications.loading, $(window).height());
  $scope.status = function (type) {
    switch (type) {
      case "ex-today":
        return moment().add(4, 'weeks').format(jsonValue.dateFormat);

      case "get-payment-method":
        var index = resourcesService.inOptions($scope.postProject.payMethod, resourcesService.paymentConfig);
        if (index == -1) return "";
        return resourcesService.paymentConfig.options[index];

      case "show-fixed-price-fields":
        if (!$scope.postProject || !$scope.postProject.payMethod) return false;
        var option = resourcesService.getOption($scope.postProject.payMethod, resourcesService.paymentConfig);
        return option.id == "fixedPrice";

      case "show-hourly-price-fields":
        if (!$scope.postProject || !$scope.postProject.payMethod) return false;
        var option = resourcesService.getOption($scope.postProject.payMethod, resourcesService.paymentConfig);
        return option.id == "hourly";

      case "is-form-valid":
        if (!$scope.postProjectForm) return true;
        $scope.postProjectForm.$setSubmitted();
        if ($scope.status("show-hourly-price-fields")) {
          $scope.hourlyForm.$setSubmitted();
        }
        else if ($scope.status("show-fixed-price-fields")) {
          $scope.fixedPriceForm.$setSubmitted();
        }
        return $scope.postProjectForm.$valid;

      case "show-estimate-workload":
        if (!$scope.hourly) return false;
        var workload = resourcesService.getOption($scope.hourly.estimatedWorkload, resourcesService.estimatedWorkloadConfig);
        if (!workload) return false;
        return workload.id !== "dontKnow";

      case "estimate-end-date-from-now":
        if (!$scope.fixedPrice) return true;
        if (!$scope.fixedPrice.estimatedEndDate) return true;
        var estimatedEndDate = moment($scope.fixedPrice.estimatedEndDate, jsonValue.dateFormat);
        if (!estimatedEndDate.isValid()) return false;
        return (estimatedEndDate.isAfter(moment(), 'day') || estimatedEndDate.isSame(moment(), "day"));

      case "hourly-rate-gt-0":
        if (!$scope.hourly) return true;
        if ($scope.hourly.hourlyRate == undefined) return true;
        return $scope.hourly.hourlyRate > 0;

      case "budget-gt-0":
        if (!$scope.fixedPrice) return true;
        if ($scope.fixedPrice.budget == undefined) return true;
        return $scope.fixedPrice.budget > 0;

      case "number-hires-bw-1-99":
        if (!$scope.postProject) return true;
        if ($scope.postProject.numberOfHires == undefined) return true;
        return $scope.postProject.numberOfHires > 0;
    }
  }

  var state = {
    default: {
      showPostProjectForm: true
    },

    review: {
      showPostProjectReview: true
    }
  }

  $scope.changeState = function (st) {
    if (!st || ($scope.state && !$scope.status("is-form-valid"))) {
      return false;
    }

    var pState = angular.copy(state[st] || st);
    $scope.state = pState;
  }

  $scope.$watch("postProject.payMethod", function (newVal, oldVal) {
    if ($scope.status("show-hourly-price-fields")) {
      $scope.hourlyForm.$setPristine();
      $rootScope.$emit("$setPristine");
    }
    else if ($scope.status("show-fixed-price-fields")) {
      $scope.fixedPriceForm.$setPristine();
      $rootScope.$emit("$setPristine");
    }
    $scope.fixedPrice = {}
    $scope.hourly = {}
  });

  $scope.createProject = function () {
    if ($scope.status("show-hourly-price-fields")) {
      $scope.hourlyForm.$setSubmitted();
    }
    if ($scope.status("show-fixed-price-fields")) {
      $scope.fixedPriceForm.$setSubmitted();
    }
    if ($scope.state && !$scope.status("is-form-valid")) {
      return false;
    }
    utils.sendNotification(jsonValue.notifications.loading, $(window).height());
    var postProject = $.extend(true, {}, $scope.hourly, $scope.fixedPrice, $scope.postProject);
    postProject.lang = $translate.use();
    apiService.postFreelancerProject(postProject)
      .success(function (projectResponse) {
        localStorageService.set("postProject", true);
        var title =  utils.toAscii($scope.postProject.projectTitle);
        return $location.url(sprintf("/freelancer/project-detail/%s-%s-id", title, projectResponse.projectId));
      }).finally(function(){
          utils.sendNotification(jsonValue.notifications.loaded);
      });
  }

  $scope.changeState('default');
  utils.sendNotification(jsonValue.notifications.loaded);
  //console.log(resourcesService.paymentConfig);
  //$scope.paymentConfig = resourcesService.paymentConfig;
});
