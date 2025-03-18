var jlab = jlab || {};
jlab.adm = jlab.adm || {};

jlab.adm.deploy = function () {
    let env = $("#env").val(),
    app = $("#app").val(),
    ver = $("#ver").val();

    let url = jlab.contextPath + "/deploy",
        data = {env: env, app: app, ver: ver};

        var promise = jlab.doAjaxJsonPostRequest(url, data, null, false);

        promise.done(function (data) {
            console.log('done', data);

            if(Object.hasOwn(data, 'exception')) {
                $("#result-message").removeClass("success");
                $("#result-message").text("Fail: " + data.exception);
            } else {
                $("#env").val("");
                $("#app").val("");
                $("#ver").val("");
                $("#result-message").addClass("success");
                $("#result-message").text("Success: Job submitted successfully. See asynchronous output for result.");
            }
        });

        promise.fail(function () {
            console.log('fail');
            $("#result-message").removeClass("success");
            $("#result-message").text("Fail: Unable to connect to adm");
        });
};

$(document).on("click", "#deploy-submit", function () {
    jlab.adm.deploy();

    return false;
});