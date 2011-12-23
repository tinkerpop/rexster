define(
    [
        "rexster/ajax",
        "rexster/history",
        "underscore",
        "rexster/graph-viz"
    ],
    elementToolbar = function (ajax, history, _, graphViz) {
        var toolbar;
        var mediator;
        var element;
        var graphName;

        var Constr = function (currentGraphName, ele, med) {
            toolbar = $("<ul/>");
            // extra css here overrides some elastic css settings
            toolbar.addClass("unit on-1 columns")
                .css({ "margin" : "0px", "margin-left" : "10px" });

            element = ele;
            mediator = med;
            graphName = currentGraphName;
        }

        Constr.prototype = {
            constructor: elementToolbar,
            version: "1.0",
            build : function() {
                return toolbar;
            },
            addNavigateButton : function(){
                var toolbarButtonGraph = toolbar.append("<li/>").children().last();

                toolbarButtonGraph.addClass("fixed column ui-state-default ui-corner-all pager-button")
                    .css({"width": "30px"});
                toolbarButtonGraph.attr("title", "View Element");

                toolbarButtonGraph.hover(function(){
                    $(this).addClass("ui-state-hover");
                    $(this).removeClass("ui-state-default");
                },
                function(){
                    $(this).addClass("ui-state-default");
                    $(this).removeClass("ui-state-hover");
                });

                var featureBrowsed = element._type == "vertex" ? "vertices" : "edges";

                var toolbarButtonGraphLink = toolbarButtonGraph.append("<a/>").children().first();
                toolbarButtonGraphLink.attr("href", "/doghouse/main/graph/" + graphName + "/" + featureBrowsed + "/" + element._id);
                toolbarButtonGraphLink.addClass("ui-icon ui-icon-arrow-4-diag");
                $(toolbarButtonGraphLink).click(function(event) {
                    event.preventDefault();
                    var uri = $(this).attr('href');
                    var split = uri.split("/");
                    history.historyPush(uri);

                    mediator.panelGraphElementViewSelected(split[5], split.slice(6).join("/"));

                });

                return this;
            },
            addVisualizationButton : function(){
                var toolbarButtonVisualizeVertex = $(toolbar.append("<li/>").children().last());
                toolbarButtonVisualizeVertex.addClass("fixed column ui-state-default ui-corner-all pager-button")
                    .css({"width": "30px"});
                toolbarButtonVisualizeVertex.attr("title", "Visualize");
                toolbarButtonVisualizeVertex.hover(function(){
                    $(this).addClass("ui-state-hover");
                    $(this).removeClass("ui-state-default");
                },
                function(){
                    $(this).addClass("ui-state-default");
                    $(this).removeClass("ui-state-hover");
                });

                var toolbarButtonVisualizeVertexLink = toolbarButtonVisualizeVertex.append("<a/>").children().first();
                toolbarButtonVisualizeVertexLink.attr("href", "/doghouse/main/graph/" + graphName + "/vertices/" + element._id);
                toolbarButtonVisualizeVertexLink.addClass("ui-icon ui-icon-zoomin");
                $(toolbarButtonVisualizeVertexLink).click(function(event) {
                    event.preventDefault();
                    var uri = $(this).attr('href');
                    var split = uri.split("/");
                    var selectedVertexIdentifier = split[6];
                    var viz;

                    $("#dialogGraphViz" ).dialog({
                        height: 625,
                        width: 625,
                        modal: true,
                        close: function(event, ui) {
                            if (typeof viz != "undefined") {
                                viz.reset();
                            }
                        }
                    });

                    ajax.getVertexBoth(graphName, selectedVertexIdentifier, function(results) {
                        var jitGraphData = _(results.results).map(function(n) {
                            return {
                                id : "" + n._id,
                                name : "" + n._id,
                                data : n,
                                adjacencies: [
                                    selectedVertexIdentifier
                                ]
                            };
                        });

                        ajax.getVertexElement(graphName, selectedVertexIdentifier, function(results){
                            jitGraphData = _([{
                                id:"" + results.results._id,
                                name:"" + results.results._id,
                                adjacencies:[],
                                data:results.results
                                }]).union(jitGraphData);
                        },null, false);


                        var handlers = {
                            onNodeRightClick : function onNodeRightClick(node) {
                                ajax.getVertexBoth(graphName, node.data._id, function (results) {
                                        var jitDataToSum = _(results.results).map(function(n) {
                                            return {
                                                id : "" + n._id,
                                                name : "" + n._id,
                                                data : n,
                                                adjacencies: [
                                                    node.data._id
                                                ]
                                            };
                                        });

                                        jitDataToSum = _([{
                                            id:"" + node.data._id,
                                            name:"" + node.data._id,
                                            adjacencies:[],
                                            data:node
                                            }]).union(jitDataToSum);

                                        viz.sum(jitDataToSum);
                                        viz.centerOnComplete(node.data._id);
                                    },
                                    function (jqXHR, textStatus, errorThrown) {
                                    }
                                );
                            }
                        };

                        viz = new graphViz("dialogGraphVizMain", jitGraphData, handlers);
                        viz.animate();
                    },
                    function(err) {

                    },
                    true);
                });

                return this;
            }
        };

        return Constr;
    });