package com.dotmarketing.portlets.rules.conditionlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.repackage.eu.bitwalker.useragentutils.UserAgent;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import com.dotmarketing.util.UtilMethods;

/**
 * This conditionlet will allow CMS users to check the platform a user request
 * comes from, such as, mobile, tablet, desktop, etc. The information is
 * obtained by reading the {@code User-Agent} header in the
 * {@link HttpServletRequest} object.
 * <p>
 * The format of the {@code User-Agent} is not standardized (basically free
 * format), which makes it difficult to decipher it. This conditionlet uses a
 * Java API called <a
 * href="http://www.bitwalker.eu/software/user-agent-utils">User Agent Utils</a>
 * which parses HTTP requests in real time and gather information about the user
 * agent, detecting a high amount of browsers, browser types, operating systems,
 * device types, rendering engines, and Web applications.
 * </p>
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 05-05-2015
 *
 */
public class UsersPlatformConditionlet extends Conditionlet {

	private static final long serialVersionUID = 1L;

	private static final String INPUT_ID = "platform";
	private static final String CONDITIONLET_NAME = "User's Platform";
	private static final String COMPARISON_IS = "is";
	private static final String COMPARISON_ISNOT = "isNot";

	private LinkedHashSet<Comparison> comparisons = null;
	private Map<String, ConditionletInput> inputValues = null;

	@Override
	protected String getName() {
		return CONDITIONLET_NAME;
	}

	@Override
	public Set<Comparison> getComparisons() {
		if (this.comparisons == null) {
			this.comparisons = new LinkedHashSet<Comparison>();
			this.comparisons.add(new Comparison(COMPARISON_IS, "Is"));
			this.comparisons.add(new Comparison(COMPARISON_ISNOT, "Is Not"));
		}
		return this.comparisons;
	}

	@Override
	public ValidationResults validate(Comparison comparison,
			Set<ConditionletInputValue> inputValues) {
		ValidationResults results = new ValidationResults();
		if (UtilMethods.isSet(inputValues)) {
			List<ValidationResult> resultList = new ArrayList<ValidationResult>();
			// Validate all available input fields
			for (ConditionletInputValue inputValue : inputValues) {
				ValidationResult validation = validate(comparison, inputValue);
				if (!validation.isValid()) {
					resultList.add(validation);
					results.setErrors(true);
				}
			}
			results.setResults(resultList);
		}
		return results;
	}

	@Override
	protected ValidationResult validate(Comparison comparison,
			ConditionletInputValue inputValue) {
		ValidationResult validationResult = new ValidationResult();
		String inputId = inputValue.getConditionletInputId();
		if (UtilMethods.isSet(inputId)) {
			String selectedValue = inputValue.getValue();
			ConditionletInput inputField = this.inputValues.get(inputId);
			validationResult.setConditionletInputId(inputId);
			Set<EntryOption> inputOptions = inputField.getData();
			for (EntryOption option : inputOptions) {
				// Validate that the selected value is correct
				if (option.getId().equals(selectedValue)) {
					validationResult.setValid(true);
					break;
				}
			}
			if (!validationResult.isValid()) {
				validationResult.setErrorMessage("Invalid value for input '"
						+ inputField.getId() + "': '" + selectedValue + "'");
			}
		}
		return validationResult;
	}

	@Override
	public Collection<ConditionletInput> getInputs(String comparisonId) {
		if (this.inputValues == null) {
			ConditionletInput inputField = new ConditionletInput();
			// Set field configuration and available options
			inputField.setId(INPUT_ID);
			inputField.setMultipleSelectionAllowed(true);
			inputField.setDefaultValue("");
			inputField.setMinNum(1);
			Set<EntryOption> options = new LinkedHashSet<EntryOption>();
			options.add(new EntryOption("computer", "Computer"));
			options.add(new EntryOption("mobile", "Mobile Device"));
			options.add(new EntryOption("tablet", "Tablet"));
			options.add(new EntryOption("wearable", "Wearable Device"));
			options.add(new EntryOption("dmr", "Digital Media Receiver"));
			options.add(new EntryOption("game_console", "Game Console"));
			inputField.setData(options);
			this.inputValues = new LinkedHashMap<String, ConditionletInput>();
			this.inputValues.put(inputField.getId(), inputField);
		}
		return this.inputValues.values();
	}

	@Override
	public boolean evaluate(HttpServletRequest request,
			HttpServletResponse response, String comparisonId,
			List<ConditionValue> values) {
		boolean result = false;
		if (UtilMethods.isSet(values) && values.size() > 0
				&& UtilMethods.isSet(comparisonId)) {
			String userAgentInfo = request.getHeader("User-Agent");
			UserAgent agent = UserAgent.parseUserAgentString(userAgentInfo);
			String platform = agent.getOperatingSystem().getDeviceType()
					.getName();
			if (UtilMethods.isSet(platform)) {
				Comparison comparison = getComparisonById(comparisonId);
				Set<ConditionletInputValue> inputValues = new LinkedHashSet<ConditionletInputValue>();
				for (ConditionValue value : values) {
					inputValues.add(new ConditionletInputValue(INPUT_ID, value
							.getValue()));
				}
				ValidationResults validationResults = validate(comparison,
						inputValues);
				if (!validationResults.hasErrors()) {
					result = evaluateInput(inputValues, platform, comparison);
				}
			}
		}
		return result;
	}

	/**
	 * Performs the comparison between the selected values in the contentlet and
	 * the platform information in the request.
	 * 
	 * @param inputValues
	 *            - The specified contentlet input values.
	 * @param platform
	 *            - The platform name obtained from the request.
	 * @param comparison
	 *            - The comparison mechanism.
	 * @return If the comparison mechanism is meets the validation criterion,
	 *         returns {@code true}. Otherwise, returns {@code false}.
	 */
	private boolean evaluateInput(Set<ConditionletInputValue> inputValues,
			String platform, Comparison comparison) {
		boolean result = false;
		if (comparison.getId().equals(COMPARISON_IS)) {
			for (ConditionletInputValue input : inputValues) {
				if (input.getValue().equalsIgnoreCase(platform)) {
					return true;
				}
			}
		} else if (comparison.getId().startsWith(COMPARISON_ISNOT)) {
			boolean found = false;
			for (ConditionletInputValue input : inputValues) {
				if (input.getValue().equalsIgnoreCase(platform)) {
					found = true;
					break;
				}
			}
			return !found;
		}
		return result;
	}

}
