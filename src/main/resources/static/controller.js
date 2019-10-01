
//Master  Module
var app = angular.module('PiCraftSwitcherApp', []);

//Master Portal Controller
var PiCraftSwitcherController = function ($scope, $http, $compile, $window, $rootScope, $q, PiCraftSwitcherService) {

 // Scope-level hook to call  API functions 
 $scope.callGetEdi = function (callback) {
     return PiCraftSwitcherService.getEdi(callback);
 };
};

//Service definition for all web service requests to the RA+ API
app.factory('PiCraftSwitcherService', ['$http', '$location', function ($http, $location) {
 return {
	 getBaseUrl: function() { return ''; },
     getStatus: function (successCallback, errorCallback) {
         $http({ method: 'GET', url: this.getBaseUrl() + '/status' })
             .success(function (data, status, headers, config) {
                 successCallback(data, status, headers, config);
             })
             .error(function (data, status, headers, config) {
                 errorCallback();
             });
         },
     getWorlds: function (successCallback, errorCallback) {
         $http({ method: 'GET', url: this.getBaseUrl() + '/worlds' })
             .success(function (data, status, headers, config) {
                 successCallback(data, status, headers, config);
             })
             .error(function (data, status, headers, config) {
                 errorCallback();
             });
         },
     postStop: function (successCallback, errorCallback) {
         $http({ method: 'POST', url: this.getBaseUrl() + '/stop' })
             .success(function (data, status, headers, config) {
                 successCallback(data, status, headers, config);
             })
             .error(function (data, status, headers, config) {
                 errorCallback();
             });
         },
	 postStartWorld: function (detail, successCallback, errorCallback) {
	     $http({ method: 'POST', url: this.getBaseUrl() + '/start/'+ detail.worldId + '/' + detail.type })
	         .success(function (data, status, headers, config) {
	             successCallback(data, status, headers, config);
	         })
	         .error(function (data, status, headers, config) {
	             errorCallback();
	         });
	     },
	 putModifyWorld: function (detail, successCallback, errorCallback) {
	     $http({ method: 'PUT', url: this.getBaseUrl() + '/modify', data: detail })
	         .success(function (data, status, headers, config) {
	             successCallback(data, status, headers, config);
	         })
	         .error(function (data, status, headers, config) {
	             errorCallback();
	         });
	     },
	 postCreateWorld: function (detail, successCallback, errorCallback) {
	     $http({ method: 'POST', url: this.getBaseUrl() + '/create', data: detail })
	         .success(function (data, status, headers, config) {
	             successCallback(data, status, headers, config);
	         })
	         .error(function (data, status, headers, config) {
	             errorCallback();
	         });
	     }
 };
}]);

// User Profile Details controller
var ViewController = function ($scope, $rootScope, $location, $interval, PiCraftSwitcherService) {

    $scope.areWorldsLoaded = false;
    $scope.worlds = [];
    $scope.isStatusLoaded = false;
    $scope.status = ["Checking..."];
    $scope.inAction = false;
    $scope.serverUp = false;
    $scope.createDetail = {
    	worldId: "",
    	levelName: "",
    	gamemode: "survival",
    	messageOfTheDay: "",
    	levelSeed: "",
    	type: "PAPER"
    };
    $scope.serverStartRequested = false;

    $scope.viewWorlds = function () {
    	$scope.inAction = true;
    	PiCraftSwitcherService.getWorlds(
                function (data, status, headers, config) {
                    //Set the scope's user profile model with the returned data.
                    $scope.worlds = data;

                    $scope.areWorldsLoaded = true;
                	$scope.inAction = false;
                },
            function () {
                $scope.areWorldsLoaded = false;
            
                $scope.worlds = [];
            	$scope.inAction = false;
            });
    };
    $scope.viewStatus = function () {
    	PiCraftSwitcherService.getStatus(
                function (data, status, headers, config) {
                    //Set the scope's user profile model with the returned data.
                    $scope.status = data;
                    if ($scope.status != null && $scope.status.length > 0) {
                    	if ($scope.status[0] == 'Server is not up!')
                    		$scope.serverUp = false;
                    	else {
                    		$scope.serverUp = true;
                    		$scope.serverStartRequested = false;
                    	}
                    }

                    $scope.isStatusLoaded = true;
                },
            function () {
                $scope.isStatusLoaded = false;
            
                $scope.status = ["Cannot read status from server."];
            });
    };

    $scope.stop = function () {
    	$scope.inAction = true;
    	PiCraftSwitcherService.postStop(
                function (data, status, headers, config) {
                    //Set the scope's user profile model with the returned data.
                    alert(data);
                	$scope.inAction = false;
                },
            function () {
                alert("Error attempting stop")
            	$scope.inAction = false;
            });
    };
    
    $scope.startWorld = function(detail) {

    	$scope.inAction = true;
    	PiCraftSwitcherService.postStartWorld(detail,
                function (data, status, headers, config) {
                    //Set the scope's user profile model with the returned data.
                    alert(data);
            		$scope.serverStartRequested = true;
                	$scope.inAction = false;
                },
            function () {
                alert("Error attempting start for world '" + detail.worldId + "'");
            	$scope.inAction = false;
            });
    };

    $scope.modifyWorld = function(detail) {

    	$scope.inAction = true;
    	PiCraftSwitcherService.putModifyWorld(detail,
                function (data, status, headers, config) {
                    //Set the scope's user profile model with the returned data.
                    alert(data);
                	$scope.inAction = false;
                	$scope.viewWorlds();
                },
            function () {
                alert("Error modifying for world '" + detail.worldId + "'");
            	$scope.inAction = false;
            	$scope.viewWorlds();
            });
    };
    $scope.createWorld = function(detail) {
       
    	$scope.inAction = true;
    	PiCraftSwitcherService.postCreateWorld(detail,
                function (data, status, headers, config) {
                    //Set the scope's user profile model with the returned data.
                    alert(data);
                	$scope.inAction = false;
                	$scope.viewWorlds();
                },
            function () {
                alert("Error creating world '" + detail.worldId + "'");
            	$scope.inAction = false;
            	$scope.viewWorlds();
            });
    };

    $scope.viewWorlds();
    $scope.viewStatus();

    $scope.intervalIstance = $interval($scope.viewStatus, 15000);
};