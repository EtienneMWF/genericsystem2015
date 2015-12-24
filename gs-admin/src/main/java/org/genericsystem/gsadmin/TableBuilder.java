package org.genericsystem.gsadmin;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.layout.Pane;

import org.genericsystem.gsadmin.RowBuilder.ExtendedRowBuilder;
import org.genericsystem.gsadmin.RowBuilder.FirstRowBuilder;
import org.genericsystem.ui.Element;
import org.genericsystem.ui.components.GSHBox;
import org.genericsystem.ui.components.GSVBox;
import org.genericsystem.ui.utils.Transformation;

public class TableBuilder<ITEM, COL> {
	<T> Table build(TableModel<ITEM, COL, T> tableModel) {
		ObservableValue<Row> firstRow = new SimpleObjectProperty<>(new FirstRowBuilder<COL>().build(tableModel.getFirstRowFirstColumnString(), tableModel.getColumns(), tableModel.getFirstRowExtractor(), tableModel.getTableStyle()));
		ObservableList<Row> rows = new Transformation<Row, ITEM>(tableModel.getItems(), item -> new RowBuilder<T, COL>().build(tableModel.getRowfirstColumnString() != null ? tableModel.getRowfirstColumnString().apply(item) : null, tableModel.getColumns(),
				tableModel.getRowColumnExtractor().apply(item), tableModel.getTableStyle()));
		return new Table(firstRow, rows, tableModel.getTableStyle());
	}

	public void init(Element<?> parent) {
		GSVBox tablePanel = new GSVBox(parent, Pane::getChildren).setPrefWidth(800).setPrefHeight(600).setStyleClass(Table::getStyleClass);
		{
			new GSHBox(tablePanel).select(Table::getFirstElement).include(new RowBuilder<>()::init).setStyleClass(Row::getStyleClass);
			new GSHBox(tablePanel).forEach(Table::getElements).include(new RowBuilder<>()::init).setStyleClass(Row::getStyleClass);
		}
	}

	public static class ExtendedTableBuilder<ITEM, COL> extends TableBuilder<ITEM, COL> {
		@Override
		public void init(Element<?> parent) {
			GSVBox tablePanel = new GSVBox(parent, Group::getChildren).setPrefWidth(800).setPrefHeight(600).setStyleClass(Table::getStyleClass);
			{
				new GSHBox(tablePanel).select(Table::getFirstElement).include(new RowBuilder<>()::init).setStyleClass(Row::getStyleClass);
				new GSHBox(tablePanel).forEach(Table::getElements).include(new ExtendedRowBuilder<>()::init).setStyleClass(Row::getStyleClass);
			}
		}
	}
}