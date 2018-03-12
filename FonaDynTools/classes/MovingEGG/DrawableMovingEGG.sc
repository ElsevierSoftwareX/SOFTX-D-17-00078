DrawableMovingEGG {
	var mData;
	var mCount;
	var mSamples;
	var normalized;

	*new { | count, samples, normalize |
		^super.new.init(count, samples, normalize.asBoolean);
	}

	init { | count, samples, normalize |
		mData = nil;
		mCount = count;
		mSamples = samples;
		normalized = normalize;
	}

	// Assumes white background
	draw { | userView |
		// Do we have data?
		if (mData.notNil, {
			// We can draw!
			Pen.use {
				var b = userView.bounds;
				var tmp;
				Pen.width = 0.01;
				if (normalized,
					{
						Pen.scale( b.width / (mSamples - 1), b.height.neg );
					}, {
						Pen.scale( b.width / (mSamples - 1), b.height.half.neg );
					}
				);
				Pen.translate( 0, -1 );

				mCount do: { | idx |
					var start = (mCount - idx - 1) * mSamples;
					var end = start + mSamples - 1;
					// Draw the frame
					Pen.strokeColor = Color.gray( (mCount - idx - 1) / mCount);

					Pen.moveTo( 0@mData[start] );
					forBy(start, end, 1, {
						| i, x |
						Pen.lineTo(x@mData[i]);
					});
					Pen.stroke;
				};
			};
		});
	}

	data_ { | arr |

		// mData = nil if arr is nil, or the array size is wrong, mData = arr
		mData = arr !? { | x |
			if ( x.size == (mCount * mSamples), x, nil)
		};
	}
}