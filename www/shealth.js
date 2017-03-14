
var exec = require('cordova/exec');

exports.connect = function(arg0, success, error) {
    exec(success, error, "Shealth", "connect", [arg0]);
};
exports.steps = function(arg0, success, error) {
    exec(success, error, "Shealth", "steps", [arg0]);
};
exports.stepsSHealth = function(arg0, success, error) {
	exec(success, error, "Shealth", "stepsSHealth", [arg0]);
}