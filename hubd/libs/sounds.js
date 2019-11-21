var sounds = {
	play: function (key) {
		try {

			if (sys.settings !== undefined && settings.get("sounds", 1) !== 1) {
				return;
			}

			if (sounds.audios === undefined) {
				sounds.audios = {};
			}
			if (sounds.audios[key] === undefined) {
				sounds.audios[key] = new Audio(constants.cdnurl + '/sounds/' + key + '.mp3');
				sounds.audios[key].load();
			}

			var audio = sounds.audios[key];

			var onCanPlay = function () {
				audio.removeEventListener('canplaythrough', onCanPlay, false);
				audio.removeEventListener('load', onCanPlay, false);
				audio.play();
			};
			audio.addEventListener('canplaythrough', onCanPlay, false);
			audio.addEventListener('load', onCanPlay, false);
			audio.load();

		} catch (e) {
		}
	}
};
