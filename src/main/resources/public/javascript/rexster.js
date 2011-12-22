require(
    [
        "domReady",
        "rexster/history",
        "rexster/template/template",
        "rexster/ui/main-menu",
        "order!has",
        "order!has-detect-features"
    ],
    function (domReady, history, template, mainMenu) {
        domReady(function () {

            // only make this feature available to browsers that support it
            if (has("native-history-state")) {
                window.onpopstate = function(event) {
                    restoreApplication();
                };
            }

            function restoreApplication() {
                // compile the templates
                template.initTemplates();

                // build the main menu.  this action will initialize the
                // first enabled panel
                mainMenu.initMainMenu();
            }


            // determine if the state is already established
            var state = history.getApplicationState();
            if (!state.hasOwnProperty("menu")) {
                // since there is no menu selected initialized the graph page first.
                history.historyPush("/doghouse/main/graph");

                if (!has("native-history-state")) {
                    restoreApplication()
                }

            } else {
                if (!has("native-history-state")) {
                    restoreApplication()
                }
            }
        });
    });