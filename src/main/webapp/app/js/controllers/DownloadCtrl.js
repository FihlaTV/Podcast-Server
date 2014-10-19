angular.module('podcast.controller')
    .controller('DownloadCtrl', function ($scope, $http, $routeParams, Restangular, podcastWebSocket, DonwloadManager, $log, Notification, $window) {
        $scope.items = Restangular.all("task/downloadManager/downloading").getList().$object;
        $scope.waitingitems = [];


        //** https://code.google.com/p/chromium/issues/detail?id=274284 **/
        // Issue fixed in the M37 of Chrome :
        $scope.activeNotification = {
            state : (('Notification' in $window) && $window.Notification.permission != 'granted'),
            manuallyactivate : Notification.requestPermission
        };


        $scope.refreshWaitingItems = function () {
            var scopeWaitingItems = $scope.waitingitems || Restangular.all("task/downloadManager/queue");
            scopeWaitingItems.getList().then(function (waitingitems) {
                $scope.waitingitems = waitingitems;
            });
        };

        Restangular.one("task/downloadManager/limit").get().then(function (data) {
            $scope.numberOfSimDl = parseInt(data);
        });

        $scope.getTypeFromStatus = function (item) {
            if (item.status === "Paused")
                return "warning";
            return "info";
        };

        $scope.updateNumberOfSimDl = DonwloadManager.updateNumberOfSimDl;

        /** Spécifique aux éléments de la liste : **/
        $scope.download = DonwloadManager.download;
        $scope.stopDownload = DonwloadManager.stopDownload;
        $scope.toggleDownload = DonwloadManager.toggleDownload;

        /** Global **/
        $scope.stopAllDownload = DonwloadManager.stopAllDownload;
        $scope.pauseAllDownload = DonwloadManager.pauseAllDownload;
        $scope.restartAllCurrentDownload = DonwloadManager.restartAllCurrentDownload;
        $scope.removeFromQueue = DonwloadManager.removeFromQueue;
        $scope.dontDonwload = DonwloadManager.dontDonwload;


        /** Websocket Connection */
        podcastWebSocket
            .subscribe("/topic/download", function (message) {
                var item = JSON.parse(message.body);
                var elemToUpdate = _.find($scope.items, { 'id': item.id });
                switch (item.status) {
                    case 'Started' :
                    case 'Paused' :
                        if (elemToUpdate)
                            _.assign(elemToUpdate, item);
                        else
                            $scope.items.push(item);
                        break;
                    case 'Finish' :
                        new Notification('Téléchargement terminé', {
                            body: item.title,
                            icon: item.cover.url,
                            delay: 5000
                        });
                    case 'Stopped' :
                        if (elemToUpdate){
                            _.remove($scope.items, function (item) {
                                return item.id === elemToUpdate.id;
                            });
                        }
                        break;
                }
        })
            .subscribe("/app/waitingList", function (message) {
                $scope.waitingitems = JSON.parse(message.body);
            })
            .subscribe("/topic/waitingList", function (message) {
                var remoteWaitingItems = JSON.parse(message.body);
                _.updateinplace($scope.waitingitems, remoteWaitingItems, function(inArray, elem) {
                    return _.findIndex(inArray, { 'id': elem.id });
                });
            });

        $scope.$on('$destroy', function () {
            podcastWebSocket
                .unsubscribe("/topic/download")
                .unsubscribe("/app/waitingList")
                .unsubscribe("/topic/waitingList");
        });

    });