package org.genericsystem.reinforcer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.genericsystem.reinforcer.tools.GSRect;

public class Labels implements Iterable<Label> {

	private final Set<Label> labels = new HashSet<>();

	public boolean addLabel(double tlx, double tly, double brx, double bry, String candidateLabel) {
		Label candidate = new Label(tlx, tly, brx, bry, candidateLabel);
		return addLabel(candidate);
	}

	public boolean addLabel(Label candidate) {
		for (Label label : labels)
			if (label.getRect().isOverlappingStrict(candidate.getRect()))
				throw new IllegalStateException(label + " intersect with : " + candidate);
		return labels.add(candidate);
	}

	@Override
	public String toString() {
		return labels.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Labels))
			return false;
		return labels.equals(((Labels) obj).labels);
	}

	@Override
	public int hashCode() {
		return labels.hashCode();
	}

	public Labels normalizeLabels() {
		double mintlx = Double.MAX_VALUE, mintly = Double.MAX_VALUE, maxbrx = 0, maxbry = 0;
		for (GSRect rect : labels.stream().map(l -> l.getRect()).collect(Collectors.toList())) {
			if (rect.getX() < mintlx)
				mintlx = rect.getX();
			if (rect.getY() < mintly)
				mintly = rect.getY();
			if (rect.br().getX() > maxbrx)
				maxbrx = rect.br().getX();
			if (rect.br().getY() > maxbry)
				maxbry = rect.br().getY();
		}
		double width = maxbrx - mintlx;
		double height = maxbry - mintly;
		Labels normalized = new Labels();
		for (Label label : labels)
			normalized.addLabel(label.normalize(mintlx, mintly, width, height));
		return normalized;
	}

	@Override
	public Iterator<Label> iterator() {
		return labels.iterator();
	}

	public Stream<Label> stream() {
		return labels.stream();
	}

	public int size() {
		return labels.size();
	}
}
