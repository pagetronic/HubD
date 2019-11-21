var title = {
	count: function (count) {
		title.reset();
		document.title = '(' + count + ') ' + document.title;
	},
	reset: function () {
		document.title = document.title.replace(/^\(([0-9]+)\) /, "");
	}
};