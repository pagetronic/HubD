if (!String.prototype.replaceAll) {
	String.prototype.replaceAll = function (search, replacement) {
		return this.replace(new RegExp(search, 'g'), replacement);
	};
}
if (!String.prototype.endsWith) {
	String.prototype.endsWith = function (searchString, position) {
		var subjectString = this.toString();
		if (typeof position !== 'number' || !isFinite(position) || Math.floor(position) !== position || position > subjectString.length) {
			position = subjectString.length;
		}
		position -= searchString.length;
		var lastIndex = subjectString.lastIndexOf(searchString, position);
		return lastIndex !== -1 && lastIndex === position;
	};
}
if (!String.prototype.startsWith) {
	String.prototype.startsWith = function (searchString, position) {
		position = position || 0;
		return this.substr(position, searchString.length) === searchString;
	};
}

if (!String.prototype.ucfirst) {
	String.prototype.ucfirst = function () {
		return this.substr(0, 1).toUpperCase() + this.substr(1, this.length);
	};
}
if (!String.prototype.unescape) {
	String.prototype.unescape = function () {
		var elem = document.createElement('textarea');
		elem.innerHTML = this;
		return elem.value;
	};
}
if (!String.prototype.autobr) {
	String.prototype.autobr = function () {
		return this.replaceAll("\n", '<br/>');
	};
}
if (!String.prototype.shuffle) {
	String.prototype.shuffle = function () {
		var a = this.split(""),
			n = a.length;

		for (var i = n - 1; i > 0; i--) {
			var j = Math.floor(Math.random() * (i + 1));
			var tmp = a[i];
			a[i] = a[j];
			a[j] = tmp;
		}
		return a.join("");
	};
}
if (!String.prototype.hashCode) {
	String.prototype.hashCode = function () {
		var hash = 0, i, chr;
		if (this.length === 0) return hash;
		for (i = 0; i < this.length; i++) {
			chr = this.charCodeAt(i);
			hash = ((hash << 5) - hash) + chr;
			hash |= 0;
		}
		return hash;
	};
}
if (!String.prototype.encrypt) {
	String.prototype.encrypt = function () {
		return Number(this.hashCode()).toString(25).replace('-', 'z');
	};
}

