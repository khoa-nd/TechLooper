techlooper.filter("challengeDetail", function (apiService, $rootScope, jsonValue, $filter) {
  return function (input, type) {
    if (!input || input.$isRich) return input;

    var challengeDetail = input;
    //var index = 0;
    //challengeDetail.criteriaIndex = 0;

    challengeDetail.refreshCriteria = function () {
      apiService.getContestDetail(challengeDetail.challengeId)
        .success(function (data) {
          challengeDetail.criteria = data.criteria;
        });
    }

    challengeDetail.refreshRegistrants = function () {
      $rootScope.$broadcast("before-getting-registrants", challengeDetail);
      apiService.getChallengeRegistrantsByPhase(challengeDetail.challengeId, challengeDetail.selectedPhaseItem.$phaseConfig.enum)
        .success(function (registrants) {
          challengeDetail.recalculateRegistrants(registrants);
        })
        .finally(function () {
          $rootScope.$broadcast("after-getting-registrants", challengeDetail);
        });
    }

    challengeDetail.saveCriteria = function () {
      challengeDetail.validateCriteria();
      if (challengeDetail.$invalidCriteria) return false;

      var criteria = {
        challengeId: challengeDetail.challengeId,
        challengeCriteria: challengeDetail.criteria
      }

      apiService.saveChallengeCriteria(criteria)
        .success(function (criteria) {
          //challengeDetail.$savedCriteria = true;
          $rootScope.$broadcast("challenge-criteria-saved", criteria);
        });
      //.error(function () {
      //  challengeDetail.$savedCriteria = false;
      //});
    }

    challengeDetail.addCriteria = function () {
      challengeDetail.criteria = challengeDetail.criteria || [];
      challengeDetail.criteria.push({index: challengeDetail.criteria.length + 1});
    }

    challengeDetail.removeCriteria = function (cri) {
      challengeDetail.criteria = _.reject(challengeDetail.criteria, function (criteria) {
        if (cri.criteriaId) return criteria.criteriaId == cri.criteriaId;
        return criteria.index == cri.index;
      });
    }

    challengeDetail.criteriaLoop = function () {
      var criteria = challengeDetail.criteria;
      if (!criteria) return [];
      challengeDetail.totalWeight = 0;
      return criteria.map(function (cri) {
        var weight = _.isNumber(cri.weight) ? cri.weight : 0;
        challengeDetail.totalWeight += weight;
        return cri;
      });
    };

    //challengeDetail.totalWeight = _.reduceRight(challengeDetail.criteria, function (sum, cri) { return sum + cri.weight; }, 0);

    challengeDetail.validateCriteria = function () {
      challengeDetail.$invalidCriteria = (challengeDetail.totalWeight != 100);
      $.each(challengeDetail.criteria, function (i, cri) {
        challengeDetail.$invalidCriteria = challengeDetail.$invalidCriteria || (!cri.name);
        return !challengeDetail.$invalidCriteria;
      });
    }

    //@see jsonValue.rewards
    challengeDetail.save1stWinner = function (registrant) {
      apiService.saveWinner(registrant.registrantId, jsonValue.rewards.firstPlaceEnum(), !registrant.firstAwarded)
        .success(function (winners) {
          challengeDetail.winners = winners;
          challengeDetail.recalculateRegistrants();
        });
    }

    challengeDetail.save2ndWinner = function (registrant) {
      apiService.saveWinner(registrant.registrantId, jsonValue.rewards.secondPlaceEnum(), !registrant.secondAwarded)
        .success(function (winners) {
          challengeDetail.winners = winners;
          challengeDetail.recalculateRegistrants();
        });
    }

    challengeDetail.save3rdWinner = function (registrant) {
      apiService.saveWinner(registrant.registrantId, jsonValue.rewards.thirdPlaceEnum(), !registrant.thirdAwarded)
        .success(function (winners) {
          challengeDetail.winners = winners;
          challengeDetail.recalculateRegistrants();
        });
    }


    //challengeDetail.recalculateWinners = function() {
    //
    //}


    challengeDetail.recalculate = function (registrants) {
      //if (!phaseName) {
      //  phaseName = challengeDetail.selectedPhaseItem ? challengeDetail.selectedPhaseItem.phase : challengeDetail.currentPhase;
      //}

      //see jsonValue.challengePhase
      var prop = jsonValue.challengePhase[challengeDetail.currentPhase].challengeProp;
      if (prop) {
        var date = moment(challengeDetail[prop], jsonValue.dateFormat);
        challengeDetail.currentPhaseDaysLeft = date.diff(moment(0, "HH"), "days") + 1;
      }

      var isOver = true;
      _.each(challengeDetail.phaseItems, function (item, index) {
        item.$index = index;
        item.isOver = isOver;
        item.phaseLowerCase = item.phase.toLowerCase();

        var cp = _.findWhere(jsonValue.challengePhase.values, {enum: item.phase});
        item.$phaseConfig = cp;
        item.countJoinerTitle = $filter("translate")(item.$phaseConfig.phaseItem.translate.countJoiner, {number: item.participant});
        item.countSubmissionTitle = $filter("translate")(item.$phaseConfig.phaseItem.translate.countSubmission, {number: item.submission});

        if (item.phase == challengeDetail.currentPhase) {
          item.isCurrentPhase = true;
          isOver = false;
        }
      });

      // mark unselectable from current-phase + 2
      var current = _.findWhere(challengeDetail.phaseItems, {isCurrentPhase: true});
      if (challengeDetail.isClosed) {
        current.isCurrentPhase = false;
        _.last(challengeDetail.phaseItems).isCurrentPhase = true;//auto select winner if challenge is closed
      }
      for (var i = current.$index + 2; i < challengeDetail.phaseItems.length; i++) {
        challengeDetail.phaseItems[i].unselectable = true;
      }

      challengeDetail.totalWeight = _.reduceRight(challengeDetail.criteria, function (sum, cri) { return sum + cri.weight; }, 0);

      if (!challengeDetail.selectedPhaseItem) {
        //if (challengeDetail.isClosed) {challengeDetail.setSelectedPhase(challengeDetail.currentPhase);}
        //else {challengeDetail.setSelectedPhase(challengeDetail.currentPhase);}
        challengeDetail.setSelectedPhase(challengeDetail.isClosed ? "WINNER" : challengeDetail.currentPhase)
      }

      if (_.isArray(registrants)) {
        //_.each(registrants, function (rgt) {
        //  rgt.ableAcceptedPhase = challengeDetail.nextPhase;
        //});
        //challengeDetail.recalculateStateWinners(registrants);
        challengeDetail.recalculateRegistrants(registrants);
      }

      //challengeDetail.recalculateRegistrantRemainsPhases(phaseName);
      //console.log(challengeDetail);
    }

    challengeDetail.recalculateRegistrants = function (registrants) {
      if (registrants) challengeDetail.$registrants = registrants;
      _.each(challengeDetail.$registrants, function (rgt, index) {
        rgt.$index = index;
        rgt.recalculate(challengeDetail);
      });

      //winner phase
      var winnerPi = _.last(challengeDetail.phaseItems);
      winnerPi.countSubmissionTitle = $filter("translate")(winnerPi.$phaseConfig.phaseItem.translate.countSubmission, {number: challengeDetail.winners.length});
    }

    challengeDetail.recalculateWinner = function (registrant) {
      if (!_.findWhere(challengeDetail.phaseItems, {phase: registrant.activePhase}).$phaseConfig.isFinal) return;

      var finalRegistrants = _.where(challengeDetail.$registrants, {activePhase: "FINAL"});
      var countWinnerPaticipants = 0;
      _.each(finalRegistrants, function (registrant) {
        if (registrant.disqualified == true) return;
        var count = _.countBy(registrant.criteria, function (cri) {
          return _.isNumber(cri.score) ? "hasScore" : "notHasScore";
        });
        countWinnerPaticipants += (count.hasScore > 0) ? 1 : 0;
      });

      var winnerPi = _.last(challengeDetail.phaseItems);
      winnerPi.participant = countWinnerPaticipants;
      winnerPi.countJoinerTitle = $filter("translate")(winnerPi.$phaseConfig.phaseItem.translate.countJoiner, {number: winnerPi.participant});
      //console.log(challengeDetail);
    }

    challengeDetail.incParticipantCount = function (registrant) {
      var pi = _.findWhere(challengeDetail.phaseItems, {phase: registrant.activePhase});
      (registrant.disqualified == false) && pi.participant++;
      pi.countJoinerTitle = $filter("translate")(pi.$phaseConfig.phaseItem.translate.countJoiner, {number: pi.participant});

      //var winnerPi = _.last(challengeDetail.phaseItems);
      challengeDetail.recalculateWinner(registrant);
      //_.countBy(registrant.criteria, function(cri) {
      //});
    }

    challengeDetail.incSubmissionCount = function (submission) {
      var pi = _.findWhere(challengeDetail.phaseItems, {phase: submission.submissionPhase});
      pi.submission++;
      pi.countSubmissionTitle = $filter("translate")(pi.$phaseConfig.phaseItem.translate.countSubmission, {number: pi.submission});
    }

    // see com.techlooper.model.ChallengeRegistrantFunnelItem
    challengeDetail.setSelectedPhase = function (phaseItem) {
      if (_.isString(phaseItem)) {
        phaseItem = _.findWhere(challengeDetail.phaseItems, {phase: phaseItem});
      }

      if (phaseItem.unselectable) return;

      challengeDetail.phaseItems.map(function (item) {item.isSelected = false;});
      challengeDetail.selectedPhaseItem = phaseItem;
      challengeDetail.$registrantsNextPhaseItem = phaseItem.$phaseConfig.isFinal ? undefined : challengeDetail.phaseItems[phaseItem.$index + 1];
      phaseItem.isSelected = true;

      challengeDetail.refreshRegistrants();
      //challengeDetail.recalculateRegistrantRemainsPhases();
    }

    challengeDetail.acceptRegistrants = function() {
      console.log(1)
    }

    challengeDetail.recalculate();

    //console.log(challengeDetail);

    challengeDetail.$isRich = true;
    return challengeDetail;
  }
});