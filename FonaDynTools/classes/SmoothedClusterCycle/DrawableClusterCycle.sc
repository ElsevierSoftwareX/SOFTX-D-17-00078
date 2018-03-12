DrawableClusterCycle {
	var <>data;
	var <>index;
	var <>count;
	var <>samples;
	var mFont;

	// Settings
	classvar fontSize = 12.0;
	classvar <errorSpace = 15.0;

	*new { | count, samples |
		^super.new.init(count, samples);
	}

	init { | count, samples |
		data = nil;
		index = nil;
		this.count = count;
		this.samples = samples;
		mFont = Font("Arial", fontSize, true);
	}

	// Assumes white background
	draw { | userView, color, bounds |
		// Do we have data?
		if (data.notNil and: index.notNil, {
			// Do we have the correct amount of data?
			if (data.size == (count * (samples + 2)), {
				// We can draw!
				Pen.use {
					var b = bounds ?? { userView.bounds.moveTo(0, 0) };
					var cd = SmoothedClusterCycle.getCycleDataRange(data, index, count, samples);

					Pen.width = 0.01;
					Pen.strokeColor = Color.grey; // color ? Color.black;
					Pen.translate( b.left, b.top );
					// format("Recent average - SqErr % (%)", cd[1].round(1e-3), cd[2].round(1e-3)).drawRightJustIn(Rect(0, 0, b.width, errorSpace), mFont, Color.grey);
					"Running average per cluster".drawRightJustIn(Rect(0, 0, b.width, 3*errorSpace), mFont, Color.grey);
					Pen.translate( 0.0, errorSpace );
					Pen.scale( b.width / (samples - 1), b.height.neg + errorSpace );
					Pen.translate( 0, -1 );

					// Draw the lines
					Pen.moveTo(0@data[cd[0][0]]);
					forBy(cd[0][0], cd[0][1], 1, {
						| i, idx |
						Pen.lineTo(idx@data[i]);
					});
					Pen.stroke;
				};
			});
		});
	}
}