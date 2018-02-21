package com.hpe.octane.ideplugins.eclipse.ui.editor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import com.hpe.adm.nga.sdk.metadata.FieldMetadata;
import com.hpe.adm.nga.sdk.model.EntityModel;
import com.hpe.adm.octane.ideplugins.services.MetadataService;
import com.hpe.adm.octane.ideplugins.services.filtering.Entity;
import com.hpe.adm.octane.ideplugins.services.util.Util;
import com.hpe.octane.ideplugins.eclipse.Activator;
import com.hpe.octane.ideplugins.eclipse.preferences.PluginPreferenceStorage;
import com.hpe.octane.ideplugins.eclipse.preferences.PluginPreferenceStorage.PreferenceConstants;
import com.hpe.octane.ideplugins.eclipse.ui.util.LinkInterceptListener;
import com.hpe.octane.ideplugins.eclipse.ui.util.PropagateScrollBrowserFactory;
import com.hpe.octane.ideplugins.eclipse.ui.util.TruncatingStyledText;
import com.hpe.octane.ideplugins.eclipse.ui.util.resource.SWTResourceManager;
import com.hpe.octane.ideplugins.eclipse.util.EntityFieldsConstants;
import org.eclipse.swt.widgets.Label;

public class EntityFieldsComposite extends Composite {

	private Color backgroundColor = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry()
			.get(JFacePreferences.CONTENT_ASSIST_BACKGROUND_COLOR);
	private Color foregroundColor = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry()
			.get(JFacePreferences.CONTENT_ASSIST_FOREGROUND_COLOR);

	private static MetadataService metadataService = Activator.getInstance(MetadataService.class);
	private Map<String, String> prettyFieldsMap;

	private Composite entityFieldsComposite;
	private Composite entityDescriptionComposite;
	private Composite fieldsComposite;

	Section sectionFields;
	Section sectionDescription;

	private ToolTip truncatedLabelTooltip;

	private FormToolkit formGenerator;

	private EntityModel entityModel;
	private Browser descBrowser;

	public EntityFieldsComposite(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(1, false));

		formGenerator = new FormToolkit(this.getDisplay());
		truncatedLabelTooltip = new ToolTip(this.getShell(), SWT.ICON_INFORMATION);

		entityFieldsComposite = new Composite(this, SWT.NONE);
		entityFieldsComposite.setForeground(SWTResourceManager.getColor(SWT.COLOR_LIST_SELECTION));
		entityFieldsComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		entityFieldsComposite.setLayout(new GridLayout(2, false));
		formGenerator.adapt(entityFieldsComposite);
		formGenerator.paintBordersFor(entityFieldsComposite);

		entityDescriptionComposite = new Composite(this, SWT.NONE);
		entityDescriptionComposite.setForeground(SWTResourceManager.getColor(SWT.COLOR_LIST_SELECTION));
		entityDescriptionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		entityDescriptionComposite.setLayout(new GridLayout(1, false));
		formGenerator.adapt(entityDescriptionComposite);
		formGenerator.paintBordersFor(entityDescriptionComposite);

		sectionFields = formGenerator.createSection(entityFieldsComposite, Section.TREE_NODE | Section.EXPANDED);
		sectionFields.setText("Fields");
		sectionFields.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		formGenerator.createCompositeSeparator(sectionFields);
		fieldsComposite = new Composite(sectionFields, SWT.NONE);
		fieldsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		sectionFields.setClient(fieldsComposite);

		sectionDescription = formGenerator.createSection(entityDescriptionComposite, Section.TREE_NODE | Section.EXPANDED);
		formGenerator.createCompositeSeparator(sectionDescription);
		sectionDescription.setText("Description");
		sectionDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		PropagateScrollBrowserFactory factory = new PropagateScrollBrowserFactory();
		descBrowser = factory.createBrowser(sectionDescription, SWT.NONE);
		descBrowser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		PluginPreferenceStorage.addPrefenceChangeHandler(PreferenceConstants.SHOWN_ENTITY_FIELDS, () -> {
			drawEntityFields(entityModel);
		});
	}

	public Section createDescriptionFormSection(EntityModel entityModel) {
		String descriptionText = "<html><body bgcolor =" + getRgbString(backgroundColor) + ">" + "<font color ="
				+ getRgbString(foregroundColor) + ">"
				+ Util.getUiDataFromModel(entityModel.getValue(EntityFieldsConstants.FIELD_DESCRIPTION))
				+ "</font></body></html>";
		if (descriptionText.equals("<html><body bgcolor =" + getRgbString(backgroundColor) + ">" + "<font color ="
				+ getRgbString(foregroundColor) + ">" + "</font></body></html>")) {
			descBrowser.setText("<html><body bgcolor =" + getRgbString(backgroundColor) + ">" + "<font color ="
					+ getRgbString(foregroundColor) + ">" + "No description" + "</font></body></html>");
		} else {
			descBrowser.setText(descriptionText);
		}

		descBrowser.addLocationListener(new LinkInterceptListener());
		sectionDescription.setClient(descBrowser);
		return sectionDescription;
	}

	private static String getRgbString(Color color) {
		return "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
	}

	private void drawEntityFields(EntityModel entityModel) {
		Set<String> shownFields = PluginPreferenceStorage.getShownEntityFields(Entity.getEntityType(entityModel));
		drawEntityFields(shownFields, entityModel);
	}

	private void drawEntityFields(Set<String> shownFields, EntityModel entityModel) {
		Arrays.stream(fieldsComposite.getChildren())
				.filter(child -> child != null)
				.filter(child -> !child.isDisposed())
				.forEach(child -> child.dispose());

		// make a map of the field names and labels
		Collection<FieldMetadata> allFields = metadataService.getVisibleFields(Entity.getEntityType(entityModel));
		allFields.stream().filter(f -> !f.getName().equals(EntityFieldsConstants.FIELD_DESCRIPTION));
		prettyFieldsMap = allFields.stream().collect(Collectors.toMap(FieldMetadata::getName, FieldMetadata::getLabel));

		fieldsComposite.setLayout(new FillLayout(SWT.HORIZONTAL));
		Composite sectionClientLeft = new Composite(fieldsComposite, SWT.NONE);
		sectionClientLeft.setLayout(new GridLayout(2, false));
		sectionClientLeft.setBackground(SWTResourceManager.getColor(SWT.COLOR_TRANSPARENT));
		Composite sectionClientRight = new Composite(fieldsComposite, SWT.NONE);
		sectionClientRight.setLayout(new GridLayout(2, false));
		sectionClientRight.setBackground(SWTResourceManager.getColor(SWT.COLOR_TRANSPARENT));

		// Skip the description field because it's in another UI component (below other fields)
		Iterator<String> iterator = shownFields.iterator();

		for (int i = 0; i < shownFields.size(); i++) {
			String fieldName = iterator.next();
			String fieldValue;

			if (EntityFieldsConstants.FIELD_OWNER.equals(fieldName)
					|| EntityFieldsConstants.FIELD_AUTHOR.equals(fieldName)
					|| EntityFieldsConstants.FIELD_TEST_RUN_RUN_BY.equals(fieldName)
					|| EntityFieldsConstants.FIELD_DETECTEDBY.equals(fieldName)) {
				fieldValue = Util.getUiDataFromModel(entityModel.getValue(fieldName),
						EntityFieldsConstants.FIELD_FULL_NAME);
			} else {
				fieldValue = Util.getUiDataFromModel(entityModel.getValue(fieldName));
			}

			// Determine if we put the label pair in the left or right container
			Composite columnComposite;
			if (i % 2 == 0) {
				columnComposite = sectionClientLeft;
			} else {
				columnComposite = sectionClientRight;
			}

			if (!fieldName.equals(EntityFieldsConstants.FIELD_DESCRIPTION) 
					&& !fieldName.equals(EntityFieldsConstants.FIELD_NAME) 
					&& !fieldName.equals(EntityFieldsConstants.FIELD_PHASE)) {
				// Add the pair of labels for field and value
				CLabel labelFieldName = new CLabel(columnComposite, SWT.NONE);
				labelFieldName.setText(prettyFieldsMap.get(fieldName));
				labelFieldName.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
				labelFieldName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

				TruncatingStyledText labelValue = new TruncatingStyledText(columnComposite, SWT.NONE, truncatedLabelTooltip);
				labelValue.setText(fieldValue);
				labelValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				labelValue.setForeground(foregroundColor);
			}
		}
		// Force redraw
		layout(true, true);
		redraw();
		update();
	}

	public EntityModel getEntityModel() {
		return entityModel;
	}

	public void setEntityModel(EntityModel entityModel) {
		this.entityModel = entityModel;
		drawEntityFields(entityModel);
		createDescriptionFormSection(entityModel);
	}

}
