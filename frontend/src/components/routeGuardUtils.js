const getAuthenticatedHomePath = (user) => {
	if (user?.role === "ADMIN") {
		return "/admin";
	}

	if (user?.role === "RECRUITER") {
		return "/recruiter/home";
	}

	if (user?.role === "PLAYER") {
		return "/player/home";
	}

	return "/dashboard";
};

const getAuthenticatedDashboardPath = (user) => {
	if (user?.role === "ADMIN") {
		return "/admin";
	}

	return "/dashboard";
};

export { getAuthenticatedHomePath, getAuthenticatedDashboardPath };