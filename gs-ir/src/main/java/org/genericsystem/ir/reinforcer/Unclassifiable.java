package org.genericsystem.ir.reinforcer;

public class Unclassifiable extends Template {

	@Override
	public double getMatchRate(Labels absoluteLabels) {
		return 1;
	}

	@Override
	public void reinforce(Labels al) {
		boolean result = absoluteLabels.add(al);
		if (result)
			System.out.println("Add Absolute labels to unclassifiable : " + absoluteLabels);
		else
			System.out.println("Absolute labels are already added to unclassifiable : " + absoluteLabels);
	}
}
