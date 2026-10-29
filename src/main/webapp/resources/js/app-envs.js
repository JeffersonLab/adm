var jlab = jlab || {};
jlab.editableRowTable = jlab.editableRowTable || {};
jlab.editableRowTable.entity = 'Deployment Allowlist Record';
jlab.editableRowTable.dialog.width = 650;
jlab.editableRowTable.dialog.height = 460;
jlab.addRow = function() {
    var appName = $("#row-app-name").val(),
        newApp = $("#row-new-app").is(":checked"),
        docUrl = $("#row-doc-url").val(),
        envName = $("#row-env-name").val(),
        requestUsername = $("#row-request-username").val(),
        deployUsername = $("#row-deploy-username").val(),
        deployHostname = $("#row-deploy-hostname").val(),
        deployPort = $("#row-deploy-port").val(),
        deployCommand = $("#row-deploy-command").val(),
        source = $("#row-form").data("copy-source"),
        reloading = false;

    // A copy must name a new App/env pair; the server refuses any other existing pair
    if (source && appName === source.appName && envName === source.envName) {
        alert('Change the App Name or Env Name: App ' + appName + ' already has an env named ' + envName);
        return;
    }

    $(".dialog-submit-button")
        .height($(".dialog-submit-button").height())
        .width($(".dialog-submit-button").width())
        .empty().append('<div class="button-indicator"></div>');
    $(".dialog-close-button").attr("disabled", "disabled");
    $(".ui-dialog-titlebar button").attr("disabled", "disabled");

    var request = jQuery.ajax({
        url: jlab.contextPath + "/inventory/ajax/add-app-env",
        type: "POST",
        data: {
            appName: appName,
            newApp: newApp ? 'Y' : 'N',
            docUrl: newApp ? docUrl : '',
            envName: envName,
            requestUsername: requestUsername,
            deployUsername: deployUsername,
            deployHostname: deployHostname,
            deployPort: deployPort,
            deployCommand: deployCommand
        },
        dataType: "json"
    });

    request.done(function(json) {
        if (json.stat === 'ok') {
            reloading = true;
            window.location.reload();
        } else {
            alert(json.error);
        }
    });

    request.fail(function(xhr, textStatus) {
        window.console && console.log('Unable to add app env; Text Status: ' + textStatus + ', Ready State: ' + xhr.readyState + ', HTTP Status Code: ' + xhr.status);
        alert('Unable to Save: Server unavailable or unresponsive');
    });

    request.always(function() {
        if (!reloading) {
            $(".dialog-submit-button").empty().text("Save");
            $(".dialog-close-button").removeAttr("disabled");
            $(".ui-dialog-titlebar button").removeAttr("disabled");
        }
    });
};
jlab.editRow = function(removeSync) {
    var appEnvId = $(".editable-row-table tr.selected-row").attr("data-id"),
        appName = $("#row-app-name").val(),
        envName = $("#row-env-name").val(),
        requestUsername = $("#row-request-username").val(),
        deployUsername = $("#row-deploy-username").val(),
        deployHostname = $("#row-deploy-hostname").val(),
        deployPort = $("#row-deploy-port").val(),
        deployCommand = $("#row-deploy-command").val(),
        reloading = false;

    $(".dialog-submit-button")
        .height($(".dialog-submit-button").height())
        .width($(".dialog-submit-button").width())
        .empty().append('<div class="button-indicator"></div>');
    $(".dialog-close-button").attr("disabled", "disabled");
    $(".ui-dialog-titlebar button").attr("disabled", "disabled");

    var request = jQuery.ajax({
        url: jlab.contextPath + "/inventory/ajax/edit-app-env",
        type: "POST",
        data: {
            appEnvId: appEnvId,
            appName: appName,
            envName: envName,
            requestUsername: requestUsername,
            deployUsername: deployUsername,
            deployHostname: deployHostname,
            deployPort: deployPort,
            deployCommand: deployCommand
        },
        dataType: "json"
    });

    request.done(function(json) {
        if (json.stat === 'ok') {
            reloading = true;
            window.location.reload();
        } else {
            alert(json.error);
        }
    });

    request.fail(function(xhr, textStatus) {
        window.console && console.log('Unable to edit app env; Text Status: ' + textStatus + ', Ready State: ' + xhr.readyState + ', HTTP Status Code: ' + xhr.status);
        alert('Unable to Save: Server unavailable or unresponsive');
    });

    request.always(function() {
        if (!reloading) {
            $(".dialog-submit-button").empty().text("Save");
            $(".dialog-close-button").removeAttr("disabled");
            $(".ui-dialog-titlebar button").removeAttr("disabled");

        }
    });
};
jlab.removeRow = function() {
    var name = $(".editable-row-table tr.selected-row td:first-child").text(),
        id = $(".editable-row-table tr.selected-row").attr("data-id"),
        reloading = false;

    $("#remove-row-button")
        .height($("#remove-row-button").height())
        .width($("#remove-row-button").width())
        .empty().append('<div class="button-indicator"></div>');

    var request = jQuery.ajax({
        url: jlab.contextPath + "/inventory/ajax/remove-app-env",
        type: "POST",
        data: {
            appEnvId: id
        },
        dataType: "json"
    });

    request.done(function(json) {
        if (json.stat === 'ok') {
            reloading = true;
            window.location.reload();
        } else {
            alert(json.error);
        }
    });

    request.fail(function(xhr, textStatus) {
        window.console && console.log('Unable to remove app; Text Status: ' + textStatus + ', Ready State: ' + xhr.readyState + ', HTTP Status Code: ' + xhr.status);
        alert('Unable to Remove Server unavailable or unresponsive');
    });

    request.always(function() {
        if (!reloading) {
            $("#remove-row-button").empty().text("Remove");
        }
    });
};
jlab.setNewApp = function(newApp) {
    $("#row-form").toggleClass("new-app", newApp);
    // A new app's name is typed, not chosen, so don't suggest existing ones
    $("#row-app-name")
        .autocomplete(newApp ? "disable" : "enable")
        .attr("placeholder", newApp ? "New app name" : "Search existing apps");
};
jlab.fillRow = function($row) {
    $("#row-app-name").val($row.attr("data-app-name"));
    $("#row-env-name").val($row.attr("data-env-name"));
    $("#row-request-username").val($row.attr("data-request-username"));
    $("#row-deploy-username").val($row.attr("data-deploy-username"));
    $("#row-deploy-hostname").val($row.attr("data-deploy-hostname"));
    $("#row-deploy-port").val($row.attr("data-deploy-port"));
    $("#row-deploy-command").val($row.attr("data-deploy-command"));
};
jlab.setCopySource = function(source) {
    $("#row-form").toggleClass("copying", !!source).data("copy-source", source);
    $("#copy-source").text(source ? source.appName + " / " + source.envName : "");
    // Make room for the copy notice
    $("#table-row-dialog").dialog("option", "height",
        jlab.editableRowTable.dialog.height + (source ? 70 : 0));
};
$(document).on("dialogclose", "#table-row-dialog", function() {
    $("#row-form")[0].reset();
    $("#row-form").removeClass("editing");
    jlab.setNewApp(false);
    // A later Add starts empty, not with the copied values
    jlab.setCopySource(null);
});
$(document).on("change", "#row-new-app", function() {
    jlab.setNewApp($(this).is(":checked"));
});
$(document).on("click", "#open-add-row-dialog-button", function() {
    $("#row-form").removeClass("editing");
    jlab.setNewApp(false);
    jlab.setCopySource(null);
});
$(document).on("click", "#open-edit-row-dialog-button", function() {
    // An edit moves the record to another existing app; Apps edits an app itself
    $("#row-form").addClass("editing");
    jlab.setNewApp(false);
    jlab.setCopySource(null);
    jlab.fillRow($(".editable-row-table tr.selected-row"));
});
$(document).on("click", "#open-copy-row-dialog-button", function() {
    // Copy fills Add from the selected record; its ID and deploy history stay with it
    var $selectedRow = $(".editable-row-table tr.selected-row");
    $("#row-form")[0].reset();
    $("#row-form").removeClass("editing");
    jlab.setNewApp(false);
    jlab.fillRow($selectedRow);
    jlab.setCopySource({
        appName: $selectedRow.attr("data-app-name"),
        envName: $selectedRow.attr("data-env-name")
    });
    // The title must start with Add, so Save adds a record rather than editing the source
    $("#table-row-dialog")
        .dialog("option", "title", "Add " + jlab.editableRowTable.entity + " (Copy)")
        .dialog("open");
    // A copy is most often a new env of the same app
    $("#row-env-name").trigger("focus").trigger("select");
});
$(document).on("table-row-add", function() {
    jlab.addRow();
});
$(document).on("table-row-edit", function() {
    jlab.editRow();
});
$(document).on("click", "#remove-row-button", function() {
    var $selectedRow = $(".editable-row-table tr.selected-row"),
        name = $selectedRow.attr("data-app-name") + " " + $selectedRow.attr("data-env-name");
    if (confirm('Are you sure you want to remove ' + name + '?')) {
        jlab.removeRow();
    }
});
$(document).on("click", ".default-clear-panel", function () {
    $("#env-name").val('');
    $("#app-name").val('');
    $("#hostname").val('');
    return false;
});
$(function(){
    $("#row-app-name").autocomplete({
        source: jlab.contextPath + "/inventory/ajax/search-apps",
        minLength: 0,
        appendTo: "#table-row-dialog"
    }).on("click", function() {
        // List the apps before anything is typed, as a dropdown would
        if (!$("#row-new-app").is(":checked") && !$(this).val()) {
            $(this).autocomplete("search", "");
        }
    });
});