techlooper.directive("submissionChallenge", function (localStorageService, apiService, $timeout, $rootScope, $location) {
  return {
    restrict: "E",
    replace: true,
    scope: {
      challenge: "=",
      form: "="
    },
    templateUrl: "modules/common/challenge/submissionChallenge.html",
    link: function (scope, el, attrs) {
      scope.form = {};

      scope.form.hideSubmitForm = function () {
        if (scope.submissionForm) {
          scope.submissionForm.$setPristine();
          scope.submissionForm.$setUntouched();
        }
        delete scope.visibleSubmitForm;
        delete scope.submission.submissionURL;
        delete scope.submission.submissionDescription;
      }

      scope.form.showSubmitForm = function () {
        localStorageService.set("submitNow", scope.challenge.challengeId);
        apiService.joinNowByFB();
      }

      var challengeId = localStorageService.get("submitNow");
      if (challengeId == scope.challenge.challengeId) {
        localStorageService.remove("submitNow");
        localStorageService.remove("joinNow");

        var firstName = localStorageService.get("firstName");
        var lastName = localStorageService.get("lastName");
        var email = localStorageService.get("email");

        apiService.findRegistrantActivePhase(challengeId, email)
          .success(function (phase) {
            scope.submission.submissionPhase = phase;
          })

        //apiService.joinContest(challengeId, firstName, lastName, email, $translate.use())
        //  .success(function() {
        scope.visibleSubmitForm = true;
        //});
        //scope.challenge.mixed = true;
      }

      //var activePhase;

      //var mixChallenge = function () {
      //  scope.challenge.hideSubmitForm = function () {
      //    if (scope.submissionForm) {
      //      scope.submissionForm.$setPristine();
      //      scope.submissionForm.$setUntouched();
      //    }
      //    delete scope.challenge.visibleSubmitForm;
      //    delete scope.submission.submissionURL;
      //    delete scope.submission.submissionDescription;
      //  }
      //
      //  scope.challenge.showSubmitForm = function () {
      //    localStorageService.set("submitNow", scope.challenge.challengeId);
      //    apiService.joinNowByFB();
      //  }
      //
      //  var challengeId = localStorageService.get("submitNow");
      //  if (challengeId == scope.challenge.challengeId) {
      //    localStorageService.remove("submitNow");
      //    localStorageService.remove("joinNow");
      //
      //    var firstName = localStorageService.get("firstName");
      //    var lastName = localStorageService.get("lastName");
      //    var email = localStorageService.get("email");
      //
      //    apiService.findRegistrantActivePhase(challengeId, email)
      //      .success(function (phase) {
      //        scope.submission.submissionPhase = phase;
      //      })
      //
      //    //apiService.joinContest(challengeId, firstName, lastName, email, $translate.use())
      //    //  .success(function() {
      //    scope.challenge.visibleSubmitForm = true;
      //    //});
      //    //scope.challenge.mixed = true;
      //  }
      //}
      //
      //if (scope.challenge) {
      //  mixChallenge();
      //}
      //else {
      //  scope.$watch("challenge", function () {
      //    if (!scope.challenge) return;
      //    mixChallenge();
      //  });
      //}


      scope.submission = {
        name: localStorageService.get("firstName") + " " + localStorageService.get("lastName"),
        registrantEmail: localStorageService.get("email"),
        registrantFirstName: localStorageService.get("firstName"),
        registrantLastName: localStorageService.get("lastName")
      }

      scope.pushChallengePhase = function () {
        scope.submissionForm.$setSubmitted();
        if (scope.submissionForm.$invalid) {
          return false;
        }
        $('.feedback-loading').css('visibility', 'inherit');
        $location.search({});
        apiService.getUrlResponseCode(scope.submission.submissionURL)
          .success(function (code) {
            var inValid = (code == 404);
            if (!inValid) {
              scope.submission.challengeId = scope.challenge.challengeId;
              $('.feedback-loading').css('visibility', 'inherit');
              apiService.submitMyResult(scope.submission)
                .success(function (data) {
                  var submission = data;
                  $rootScope.$broadcast("submission-accepted", submission);
                })
                .finally(function () {
                  $timeout(function () {
                    $('.feedback-loading').css('visibility', 'hidden');
                    scope.challenge.hideSubmitForm();
                  }, 500);
                });
            }
            scope.submissionForm && scope.submissionForm.submissionURL.$setValidity("invalidUrl", !inValid);
          })
          .finally(function () {
            $timeout(function () {
              $('.feedback-loading').css('visibility', 'hidden');
            }, 500);
          });
      }
    }
  }
});